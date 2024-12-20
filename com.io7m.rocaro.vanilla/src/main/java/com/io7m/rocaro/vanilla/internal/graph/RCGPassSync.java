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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Execute;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.MemoryReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.ReadType;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Submission;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Write;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.WriteType;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.MaskSubgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;

/**
 * A pass that computes the set of distinct barriers required to synchronize
 * operations in the graph. No attempt is made to merge barriers for efficiency.
 */

public final class RCGPassSync
  implements RCGGraphPassType
{
  private DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph;
  private HashMap<RCGOperationType, Execute> opCommands;
  private DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> portGraph;

  /**
   * A pass that computes the set of distinct barriers required to synchronize
   * operations in the graph. No attempt is made to merge barriers for efficiency.
   */

  public RCGPassSync()
  {

  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    this.portGraph = builder.graph();
    this.syncGraph = builder.syncGraph();
    this.opCommands = builder.syncOpCommands();

    final var submission =
      new Submission(GRAPHICS, 0);

    for (final var op : builder.opsOrdered()) {
      this.processOp(builder, submission, op);
    }
  }

  private void addDependencyBeforeAfter(
    final RCGSyncCommandType before,
    final RCGSyncCommandType after)
  {
    this.syncGraph.addVertex(before);
    this.syncGraph.addVertex(after);

    /*
     * Note the reversed (after, before): after _depends on_ before, so the
     * edge points from the after operation to the before operation.
     */

    this.syncGraph.addEdge(after, before, new RCGSyncDependency(before, after));
  }

  private void processOp(
    final RCGGraphBuilderInternalType builder,
    final Submission submission,
    final RCGOperationType op)
  {
    final var portResources =
      builder.portResourcesTracked();

    final var opCmd = new Execute(submission, op);
    this.syncGraph.addVertex(opCmd);

    for (final var port : op.ports().values()) {
      final var resource = portResources.get(port);
      for (final var read : port.readsOnStages()) {
        final var r = opCmd.addRead(resource, read);
        this.addDependencyBeforeAfter(r, opCmd);
      }
      for (final var write : port.writesOnStages()) {
        final var w = opCmd.addWrite(resource, write);
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
    final Execute opCmd,
    final RCGResourcePlaceholderType resource,
    final RCGOperationImageLayoutTransitionType imageLayoutTransition,
    final RCGPortType port)
  {
    this.processPortReadBarriers(
      opCmd,
      resource,
      imageLayoutTransition,
      port
    );

    this.processPortWriteBarriers(
      opCmd,
      resource,
      imageLayoutTransition,
      port
    );
  }

  private void processPortWriteBarriers(
    final RCGSyncCommandType.Execute opCmd,
    final RCGResourcePlaceholderType resource,
    final RCGOperationImageLayoutTransitionType imageLayoutTransition,
    final RCGPortType port)
  {
    /*
     * If we have a pre execution layout transition, then our writes must
     * depend on it, and it must depend on incoming writes.
     */

    final var layoutPreOpt = imageLayoutTransition.pre();
    if (layoutPreOpt.isPresent()) {
      final var layoutPre =
        layoutPreOpt.get();

      final var portIter =
        this.portGraph.incomingEdgesOf(port)
          .iterator();

      if (!portIter.hasNext()) {
        throw new UnreachableCodeException();
      }

      final RCGSyncCommandType.Execute opSrcCmd =
        this.opCommands.get(portIter.next().sourcePort().owner());
      final var myLeafWrites =
        this.leafWritesFor(opCmd, resource);

      for (final var myWrite : myLeafWrites) {
        final var theirLeafWrites =
          this.leafWritesFor(opSrcCmd, resource);

        for (final var theirWrite : theirLeafWrites) {
          final var barrier =
            new RCGSyncCommandType.ImageWriteBarrier(
              opCmd,
              resource,
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

    /*
     * If we have a post execution layout transition, then it must depend
     * on each of our writes.
     */

    final var layoutPostOpt = imageLayoutTransition.post();
    if (layoutPostOpt.isPresent()) {
      final var layoutPost =
        layoutPostOpt.get();
      final var leafWrites =
        this.leafWritesFor(opCmd, resource);

      for (final var myWrite : leafWrites) {
        final var barrier =
          new RCGSyncCommandType.ImageWriteBarrier(
            opCmd,
            resource,
            myWrite.writeStage(),
            myWrite.writeStage(),
            layoutPost.layoutFrom(),
            layoutPost.layoutTo()
          );

        this.addDependencyBeforeAfter(myWrite, barrier);
      }
    }
  }

  private void processPortReadBarriers(
    final RCGSyncCommandType.Execute opCmd,
    final RCGResourcePlaceholderType resource,
    final RCGOperationImageLayoutTransitionType imageLayoutTransition,
    final RCGPortType port)
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
      final var layoutPre =
        layoutPreOpt.get();

      final var leafWrites =
        this.leafWritesFor(opSrcCmd, resource);

      for (final var myRead : opCmd.reads()) {
        for (final var w : leafWrites) {
          final var cmdLayout =
            new RCGSyncCommandType.ImageReadBarrier(
              opCmd,
              resource,
              w.writeStage(),
              myRead.readsAt(),
              layoutPre.layoutFrom(),
              layoutPre.layoutTo()
            );

          this.addDependencyBeforeAfter(w, cmdLayout);
          this.addDependencyBeforeAfter(cmdLayout, myRead);
        }
      }
      return;
    }

    /*
     * We have no layout transition that happens before execution, so our
     * reads must have read barriers so that writes by dependencies
     * happen-before our reads.
     */

    final var leafWrites =
      this.leafWritesFor(opSrcCmd, resource);

    for (final var myRead : opCmd.reads()) {
      for (final var w : leafWrites) {
        final var barrier =
          new MemoryReadBarrier(
            opCmd,
            resource,
            w.writeStage(),
            myRead.readsAt()
          );

        this.addDependencyBeforeAfter(barrier, myRead);
        this.addDependencyBeforeAfter(w, barrier);
      }
    }
  }

  private Set<WriteType> leafWritesFor(
    final RCGSyncCommandType.Execute cmd,
    final RCGResourcePlaceholderType resource)
  {
    final var r = new HashSet<WriteType>(cmd.writes().size());
    for (final var w : cmd.writes()) {
      if (Objects.equals(w.resource(), resource)) {
        r.add(this.leafOf(w));
      }
    }
    return Set.copyOf(r);
  }

  private WriteType leafOf(
    final Write w)
  {
    WriteType now = w;

    /*
     * Construct a subgraph view that only considers commands with the
     * same owner as the write command.
     */

    final var subgraph =
      new MaskSubgraph<>(
        this.syncGraph,
        c -> {
          return switch (c) {
            case final Execute cc -> {
              yield !cc.equals(w.owner());
            }
            case final Submission cc -> {
              yield false;
            }
            case final ReadType cc -> {
              yield !Objects.equals(cc.owner(), w.owner());
            }
            case final WriteType cc -> {
              yield !Objects.equals(cc.owner(), w.owner());
            }
          };
        },
        _ -> false
      );

    while (true) {
      final var edges = subgraph.incomingEdgesOf(now);
      if (edges.isEmpty()) {
        return now;
      }
      now = (WriteType) edges.iterator().next().after();
    }
  }
}
