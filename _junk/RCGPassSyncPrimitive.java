/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.rocaro.vanilla.internal.graph.sync_primitive;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImageType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphPassType;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.MaskSubgraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A pass that computes the set of distinct barriers required to synchronize
 * operations in the graph. No attempt is made to merge barriers for efficiency.
 */

public final class RCGPassSyncPrimitive
  implements RCGGraphPassType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGPassSyncPrimitive.class);

  private int submissionIdNext;
  private long commandIdNext;
  private DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph;
  private HashMap<RCGOperationType, RCGSExecute> opCommands;
  private DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> portGraph;

  /**
   * A pass that computes the set of distinct barriers required to synchronize
   * operations in the graph. No attempt is made to merge barriers for efficiency.
   */

  public RCGPassSyncPrimitive()
  {
    this.commandIdNext = 0L;
    this.submissionIdNext = 0;
  }

  private long freshId()
  {
    final var r = this.commandIdNext;
    this.commandIdNext = this.commandIdNext + 1L;
    return r;
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    this.portGraph =
      builder.primitivePortGraph();
    this.syncGraph =
      builder.syncGraph();
    this.opCommands =
      builder.syncOpCommands();

    /*
     * The first pass inserts barriers into the graph, but doesn't take into
     * account anything to do with queue ownership transfers.
     */

    for (final var op : builder.opsOrdered()) {
      this.processOp(builder, op);
    }

    /*
     * The second pass annotates all operations with the Vulkan queue submission
     * in which they'll be placed.
     */

    this.processSubmissions();

    for (final var op : this.syncGraph.vertexSet()) {
      Postconditions.checkPostconditionV(
        op.submission(),
        op.submission() != null,
        "Submission must be assigned to %s.",
        op
      );
    }

    /*
     * The third pass adjusts barriers to turn them into semaphore-blocked
     * queue ownership transfers, where applicable.
     */

    this.processSemaphores();
  }

  private void replaceBarrier(
    final RCGSBarrierType oldBarrier,
    final RCGSBarrierType newBarrier)
  {
    final var incomingEdges =
      Set.copyOf(this.syncGraph.incomingEdgesOf(oldBarrier));
    final var outgoingEdges =
      Set.copyOf(this.syncGraph.outgoingEdgesOf(oldBarrier));

    this.syncGraph.removeVertex(oldBarrier);
    this.syncGraph.addVertex(newBarrier);

    for (final var edge : incomingEdges) {
      this.addEquivalentBarrierEdge(edge, oldBarrier, newBarrier);
    }
    for (final var edge : outgoingEdges) {
      this.addEquivalentBarrierEdge(edge, oldBarrier, newBarrier);
    }

    Postconditions.checkPostconditionV(
      this.syncGraph.containsVertex(newBarrier),
      "Graph must contain new barrier %s",
      newBarrier
    );
    Postconditions.checkPostconditionV(
      !this.syncGraph.containsVertex(oldBarrier),
      "Graph must not contain old barrier %s",
      oldBarrier
    );
  }

  private void addEquivalentBarrierEdge(
    final RCGSyncDependency oldEdge,
    final RCGSBarrierType oldBarrier,
    final RCGSBarrierType newBarrier)
  {
    if (Objects.equals(oldEdge.source(), oldBarrier)) {
      final var newEdge = new RCGSyncDependency(newBarrier, oldEdge.target());
      LOG.trace("Replace ({}) with ({})", oldEdge, newEdge);
      this.syncGraph.addEdge(newBarrier, oldEdge.target(), newEdge);
    } else if (Objects.equals(oldEdge.target(), oldBarrier)) {
      final var newEdge = new RCGSyncDependency(oldEdge.source(), newBarrier);
      LOG.trace("Replace ({}) with ({})", oldEdge, newEdge);
      this.syncGraph.addEdge(oldEdge.source(), newBarrier, newEdge);
    } else {
      throw new UnreachableCodeException();
    }
  }

  private void processSemaphores()
  {
    final var vertices =
      new HashSet<>(this.syncGraph.vertexSet());

    for (final var c : vertices) {
      if (c instanceof final RCGSBarrierType barrier) {
        this.processSemaphoreBarrier(barrier);
      }
    }
  }

  private void processSemaphoreBarrier(
    final RCGSBarrierType barrier)
  {
    switch (barrier) {
      case final RCGSReadBarrierType readBarrier -> {
        this.processSemaphoreBarrierRead(readBarrier);
      }
      case final RCGSWriteBarrierType writeBarrier -> {
        this.processSemaphoreBarrierWrite(writeBarrier);
      }
    }
  }

  private void processSemaphoreBarrierWrite(
    final RCGSWriteBarrierType writeBarrier)
  {
    if (writeBarrier instanceof RCGSBarrierWithQueueTransferType) {
      return;
    }

    final var targetSubmission =
      writeBarrier.submission();

    final var incomingEdges =
      this.syncGraph.incomingEdgesOf(writeBarrier);
    final var outgoingEdges =
      this.syncGraph.outgoingEdgesOf(writeBarrier);

    Invariants.checkInvariantV(
      writeBarrier,
      !incomingEdges.isEmpty(),
      "Barriers must have incoming edges."
    );
    Invariants.checkInvariantV(
      writeBarrier,
      !outgoingEdges.isEmpty(),
      "Barriers must have outgoing edges."
    );

    for (final var edge : incomingEdges) {
      Invariants.checkInvariantV(
        writeBarrier == edge.target(),
        "Write barrier %s must match the target of edge %s.",
        writeBarrier,
        edge
      );
    }

    /*
     * A write barrier becomes a queue transfer if the source of an incoming
     * edge is on a different queue category.
     */

    final var sourceSubmission =
      incomingEdges.iterator().next().source().submission();

    if (Objects.equals(sourceSubmission, targetSubmission)) {
      return;
    }

    final var sourceQueue =
      sourceSubmission.queue();

    final var newBarrier =
      switch (writeBarrier) {
        case final RCGSImageWriteBarrier b -> {
          yield new RCGSImageWriteBarrierWithQueueTransfer(
            b.commandId(),
            b.owner(),
            b.resource(),
            b.waitsForWriteAt(),
            b.blocksWriteAt(),
            b.layoutFrom(),
            b.layoutTo(),
            sourceSubmission,
            targetSubmission
          );
        }

        case final RCGSMemoryWriteBarrier b -> {
          yield new RCGSMemoryWriteBarrierWithQueueTransfer(
            b.commandId(),
            b.owner(),
            b.resource(),
            b.waitsForWriteAt(),
            b.blocksWriteAt(),
            sourceSubmission,
            targetSubmission
          );
        }

        case final RCGSImageWriteBarrierWithQueueTransfer _,
             final RCGSMemoryWriteBarrierWithQueueTransfer _ ->
          throw new UnreachableCodeException();
      };

    newBarrier.setSubmission(writeBarrier.submission());
    this.replaceBarrier(writeBarrier, newBarrier);
  }

  private void processSemaphoreBarrierRead(
    final RCGSReadBarrierType readBarrier)
  {
    if (readBarrier instanceof RCGSBarrierWithQueueTransferType) {
      return;
    }

    final var targetSubmission =
      readBarrier.submission();

    final var incomingEdges =
      this.syncGraph.incomingEdgesOf(readBarrier);
    final var outgoingEdges =
      this.syncGraph.outgoingEdgesOf(readBarrier);

    Invariants.checkInvariantV(
      readBarrier,
      !incomingEdges.isEmpty(),
      "Barriers must have incoming edges."
    );
    Invariants.checkInvariantV(
      readBarrier,
      !outgoingEdges.isEmpty(),
      "Barriers must have outgoing edges."
    );

    for (final var edge : incomingEdges) {
      Invariants.checkInvariantV(
        readBarrier == edge.target(),
        "Read barrier %s must match the target of edge %s.",
        readBarrier,
        edge
      );
    }

    /*
     * A read barrier becomes a queue transfer if the source of an incoming
     * edge is on a different queue category.
     */

    final var sourceSubmission =
      incomingEdges.iterator().next().source().submission();

    if (Objects.equals(sourceSubmission, targetSubmission)) {
      return;
    }

    final var sourceQueue =
      sourceSubmission.queue();

    final var newBarrier =
      switch (readBarrier) {
        case final RCGSImageReadBarrier b -> {
          yield new RCGSImageReadBarrierWithQueueTransfer(
            b.commandId(),
            b.owner(),
            b.resource(),
            b.waitsForWriteAt(),
            b.blocksReadAt(),
            b.layoutFrom(),
            b.layoutTo(),
            sourceSubmission,
            targetSubmission
          );
        }
        case final RCGSMemoryReadBarrier b -> {
          yield new RCGSMemoryReadBarrierWithQueueTransfer(
            b.commandId(),
            b.owner(),
            b.resource(),
            b.waitsForWriteAt(),
            b.blocksReadAt(),
            sourceSubmission,
            targetSubmission
          );
        }
        case final RCGSImageReadBarrierWithQueueTransfer _,
             final RCGSMemoryReadBarrierWithQueueTransfer _ -> {
          throw new UnreachableCodeException();
        }
      };

    newBarrier.setSubmission(readBarrier.submission());
    this.replaceBarrier(readBarrier, newBarrier);
  }

  private void processSubmissions()
  {
    /*
     * We need to split the graph up into separate islands based on the
     * queue category of each operation. The islands represent the unique
     * submissions that need to be made to each Vulkan queue.
     */

    final var graphCopy =
      (Graph<RCGSyncCommandType, RCGSyncDependency>) this.syncGraph.clone();

    final var removeEdges =
      new HashSet<RCGSyncDependency>();

    for (final var edge : graphCopy.edgeSet()) {
      final var op0 =
        edge.source().operation();
      final var op1 =
        edge.target().operation();

      if (op0.queueCategory() != op1.queueCategory()) {
        removeEdges.add(edge);
      }
    }

    for (final var edge : removeEdges) {
      graphCopy.removeEdge(edge);
    }

    final var connectivity =
      new ConnectivityInspector<>(graphCopy);

    for (final var graphIsland : connectivity.connectedSets()) {
      if (!graphIsland.isEmpty()) {
        final var first =
          graphIsland.iterator().next();
        final var submission =
          this.allocateSubmission(first.operation().queueCategory());

        for (final var operation : graphIsland) {
          operation.setSubmission(submission);
        }
      }
    }
  }

  private RCGSubmissionID allocateSubmission(
    final RCDeviceQueueCategory queue)
  {
    final var submission = new RCGSubmissionID(queue, this.submissionIdNext);
    this.submissionIdNext = this.submissionIdNext + 1;
    return submission;
  }

  private void addDependencyBeforeAfter(
    final RCGSyncCommandType before,
    final RCGSyncCommandType after)
  {
    this.syncGraph.addVertex(before);
    this.syncGraph.addVertex(after);
    this.syncGraph.addEdge(before, after, new RCGSyncDependency(before, after));
  }

  private void processOp(
    final RCGGraphBuilderInternalType builder,
    final RCGOperationType op)
  {
    final var portResources =
      builder.portResourcesTracked();

    /*
     * Instantiate an Execute command. Make our writes depend on it, and
     * make it depend on our reads.
     */

    final var opCmd = new RCGSExecute(this.freshId(), op);
    this.syncGraph.addVertex(opCmd);

    for (final var port : op.ports().values()) {
      final var resource = portResources.get(port);
      for (final var read : port.readsOnStages()) {
        final var r = opCmd.addRead(this.freshId(), resource, read);
        this.addDependencyBeforeAfter(r, opCmd);
      }
      for (final var write : port.writesOnStages()) {
        final var w = opCmd.addWrite(this.freshId(), resource, write);
        this.addDependencyBeforeAfter(opCmd, w);
      }
    }

    Preconditions.checkPreconditionV(
      !this.opCommands.containsKey(op),
      "Operations must be processed once."
    );
    this.opCommands.put(op, opCmd);

    for (final var port : op.ports().values()) {
      final var resource =
        portResources.get(port);
      final var imageLayoutTransition =
        builder.portImageLayouts().get(port);

      this.processPort(opCmd, resource, imageLayoutTransition, port);
    }
  }

  private void processPort(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGOperationImageLayoutTransitionType transition,
    final RCGPortType<?> port)
  {
    this.processPortReadBarriers(opCmd, resource, transition, port);
    this.processPortWriteBarriers(opCmd, resource, transition, port);
  }

  private void processPortWriteBarriers(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGOperationImageLayoutTransitionType imageLayoutTransition,
    final RCGPortType<?> port)
  {
    /*
     * If we have a pre execution layout transition, then our writes must
     * depend on it, and it must depend on incoming writes.
     */

    final var layoutPreOpt = imageLayoutTransition.pre();
    if (layoutPreOpt.isPresent()) {
      this.processPortWriteBarriersLayoutPre(
        opCmd,
        resource,
        port,
        layoutPreOpt.get()
      );
    } else {

      /*
       * We have no layout transition, but we may have writes to perform.
       * Those write commands implicitly depend on writes upon which the
       * operation depends, so we need to insert explicit write barriers
       * for resources that need them.
       */

      Preconditions.checkPreconditionV(
        resource.schematic(),
        resource.schematic() instanceof RCResourceSchematicBufferType,
        "Resource schematic must be of a buffer type."
      );

      final var resourceTyped =
        (RCGResourceVariable<? extends RCResourceSchematicBufferType>) resource;

      final var myLeafWrites =
        this.leafWritesFor(opCmd, resource);
      final var connections =
        List.copyOf(this.portGraph.incomingEdgesOf(port));

      for (final var connection : connections) {
        final var opSrcCmd =
          this.opCommands.get(connection.sourcePort().owner());

        final var theirWrites =
          this.leafWritesFor(opSrcCmd, resource);

        for (final var myWrite : myLeafWrites) {
          for (final var theirWrite : theirWrites) {
            final var barrier =
              new RCGSMemoryWriteBarrier(
                this.freshId(),
                opCmd,
                resourceTyped,
                theirWrite.writeStage(),
                myWrite.writeStage()
              );

            this.addDependencyBeforeAfter(barrier, myWrite);
            this.addDependencyBeforeAfter(theirWrite, barrier);
          }
        }
      }
    }

    /*
     * If we have a post execution layout transition, then it must depend
     * on each of our writes.
     */

    final var layoutPostOpt = imageLayoutTransition.post();
    layoutPostOpt.ifPresent(post -> {
      this.processPortWriteBarriersLayoutPost(opCmd, resource, post);
    });
  }

  private void processPortWriteBarriersLayoutPost(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGOperationImageLayoutTransitionType.Post layoutPost)
  {
    Preconditions.checkPreconditionV(
      resource.schematic(),
      resource.schematic() instanceof RCResourceSchematicImageType,
      "Resource schematic must be of an image type."
    );

    final var resourceTyped =
      (RCGResourceVariable<? extends RCResourceSchematicImageType>) resource;

    final var leafWrites =
      this.leafWritesFor(opCmd, resource);

    for (final var myWrite : leafWrites) {
      final var barrier =
        new RCGSImageWriteBarrier(
          this.freshId(),
          opCmd,
          resourceTyped,
          myWrite.writeStage(),
          myWrite.writeStage(),
          layoutPost.layoutFrom(),
          layoutPost.layoutTo()
        );

      this.addDependencyBeforeAfter(myWrite, barrier);
    }
  }

  private void processPortWriteBarriersLayoutPre(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGPortType<?> port,
    final RCGOperationImageLayoutTransitionType.Pre layoutPre)
  {
    Preconditions.checkPreconditionV(
      resource.schematic(),
      resource.schematic() instanceof RCResourceSchematicImageType,
      "Resource schematic must be of an image type."
    );

    final var resourceTyped =
      (RCGResourceVariable<? extends RCResourceSchematicImageType>) resource;

    final var connections =
      List.copyOf(this.portGraph.incomingEdgesOf(port));

    Invariants.checkInvariantV(
      !connections.isEmpty(),
      "Port connections cannot be empty"
    );

    final var opSrcCmd =
      this.opCommands.get(connections.getFirst().sourcePort().owner());
    final var myLeafWrites =
      this.leafWritesFor(opCmd, resource);

    for (final var myWrite : myLeafWrites) {
      final var theirLeafWrites =
        this.leafWritesFor(opSrcCmd, resource);

      for (final var theirWrite : theirLeafWrites) {
        final var barrier =
          new RCGSImageWriteBarrier(
            this.freshId(),
            opCmd,
            resourceTyped,
            theirWrite.writeStage(),
            myWrite.writeStage(),
            layoutPre.layoutFrom(),
            layoutPre.layoutTo()
          );

        this.addDependencyBeforeAfter(theirWrite, barrier);
        this.addDependencyBeforeAfter(barrier, myWrite);
      }
    }
  }

  private void processPortReadBarriers(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGOperationImageLayoutTransitionType imageLayoutTransition,
    final RCGPortType<?> port)
  {
    final var portIter =
      this.portGraph.incomingEdgesOf(port)
        .iterator();

    if (!portIter.hasNext()) {
      return;
    }

    final var connection =
      portIter.next();

    final var opSrcCmd =
      this.opCommands.get(connection.sourcePort().owner());

    Objects.requireNonNull(opSrcCmd, "opSrcCmd");

    /*
     * If we have a layout transition that occurs prior to execution of
     * the operation, then the layout transition happens-before any reads
     * we have, and any writes by dependencies happen-before the layout
     * transition.
     */

    final var layoutPreOpt = imageLayoutTransition.pre();
    if (layoutPreOpt.isPresent()) {
      this.processPortReadBarriersLayoutPre(
        opCmd,
        resource,
        opSrcCmd,
        layoutPreOpt.get()
      );
      return;
    }

    /*
     * We have no layout transition that happens before execution, so our
     * reads must have read barriers so that writes by dependencies
     * happen-before our reads.
     */

    final var leafWrites =
      this.leafWritesFor(opSrcCmd, resource);

    Preconditions.checkPreconditionV(
      resource.schematic(),
      resource.schematic() instanceof RCResourceSchematicBufferType,
      "Resource schematic must be of a buffer type."
    );

    final var resourceTyped =
      (RCGResourceVariable<? extends RCResourceSchematicBufferType>) resource;

    for (final var myRead : opCmd.reads()) {
      for (final var w : leafWrites) {
        final var barrier =
          new RCGSMemoryReadBarrier(
            this.freshId(),
            opCmd,
            resourceTyped,
            w.writeStage(),
            myRead.readsAt()
          );

        this.addDependencyBeforeAfter(barrier, myRead);
        this.addDependencyBeforeAfter(w, barrier);
      }
    }
  }

  private void processPortReadBarriersLayoutPre(
    final RCGSExecute opCmd,
    final RCGResourceVariable<?> resource,
    final RCGSExecute opSrcCmd,
    final RCGOperationImageLayoutTransitionType.Pre layoutPre)
  {
    Preconditions.checkPreconditionV(
      resource.schematic(),
      resource.schematic() instanceof RCResourceSchematicImageType,
      "Resource schematic must be of an image type."
    );

    final var resourceTyped =
      (RCGResourceVariable<? extends RCResourceSchematicImageType>) resource;

    final var leafWrites =
      this.leafWritesFor(opSrcCmd, resource);

    for (final var myRead : opCmd.reads()) {
      for (final var w : leafWrites) {
        final var cmdLayout =
          new RCGSImageReadBarrier(
            this.freshId(),
            opCmd,
            resourceTyped,
            w.writeStage(),
            myRead.readsAt(),
            layoutPre.layoutFrom(),
            layoutPre.layoutTo()
          );
        this.addDependencyBeforeAfter(w, cmdLayout);
        this.addDependencyBeforeAfter(cmdLayout, myRead);
      }
    }
  }

  private Set<RCGSWriteType> leafWritesFor(
    final RCGSExecute cmd,
    final RCGResourceVariable<?> resource)
  {
    final var r = new HashSet<RCGSWriteType>(cmd.writes().size());
    for (final var w : cmd.writes()) {
      if (Objects.equals(w.resource(), resource)) {
        r.add(this.leafOf(w));
      }
    }
    return Set.copyOf(r);
  }

  private RCGSWriteType leafOf(
    final RCGSWrite w)
  {
    RCGSWriteType now = w;

    /*
     * Construct a subgraph view that only considers commands with the
     * same owner as the write command.
     */

    final var subgraph =
      new MaskSubgraph<>(
        this.syncGraph,
        c -> {
          return switch (c) {
            case final RCGSExecute cc -> {
              yield !cc.equals(w.owner());
            }
            case final RCGSReadType cc -> {
              yield !Objects.equals(cc.owner(), w.owner());
            }
            case final RCGSWriteType cc -> {
              yield !Objects.equals(cc.owner(), w.owner());
            }
          };
        },
        _ -> false
      );

    /*
     * The "leaf" write is the write command that has no outgoing edges.
     */

    while (true) {
      final var edges = subgraph.outgoingEdgesOf(now);
      if (edges.isEmpty()) {
        return now;
      }
      now = (RCGSWriteType) edges.iterator().next().target();
    }
  }
}
