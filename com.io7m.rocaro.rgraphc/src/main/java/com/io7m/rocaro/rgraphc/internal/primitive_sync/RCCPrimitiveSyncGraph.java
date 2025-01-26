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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCImageLayoutStatusType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderImageType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderMemoryType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortImageLayout;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveConnection;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveProducer;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPrimitivePortGraph;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;
import com.io7m.seltzer.api.SStructuredError;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonPropertyOrder(alphabetic = true)
public final class RCCPrimitiveSyncGraph
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCCPrimitiveSyncGraph.class);

  private final Builder builder;

  private RCCPrimitiveSyncGraph(
    final Builder inBuilder)
  {
    this.builder =
      Objects.requireNonNull(inBuilder, "builder");
  }

  public static RCCPrimitiveSyncGraph create(
    final RCTGraphDeclarationType graph,
    final RCCPrimitivePortGraph portGraph)
    throws RCCompilerException
  {
    return new Builder(graph, portGraph).build();
  }

  public Graph<RCCCommandType, RCCSyncDependency> graph()
  {
    return new AsUnmodifiableGraph<>(this.builder.syncGraph);
  }

  @JsonProperty("Commands")
  public SortedMap<Long, RCCCommandType> commands()
  {
    return Collections.unmodifiableSortedMap(this.builder.commands);
  }

  @JsonProperty("Connections")
  public Set<RCCSyncDependency> connections()
  {
    return Collections.unmodifiableSortedSet(
      new TreeSet<>(this.builder.syncGraph.edgeSet())
    );
  }

  private static final class Builder
  {
    private final HashMap<RCTOperationDeclaration, RCCExecute> opCommands;
    private final TreeMap<Long, RCCCommandType> commands;
    private final RCCPrimitivePortGraph portGraph;
    private final RCTGraphDeclarationType graph;
    private DirectedAcyclicGraph<RCCCommandType, RCCSyncDependency> syncGraph;
    private long commandIdNext;
    private int submissionIdNext;

    Builder(
      final RCTGraphDeclarationType inGraph,
      final RCCPrimitivePortGraph inPortGraph)
    {
      this.portGraph =
        Objects.requireNonNull(inPortGraph, "portGraph");
      this.graph =
        Objects.requireNonNull(inGraph, "inGraph");
      this.syncGraph =
        new DirectedAcyclicGraph<>(RCCSyncDependency.class);
      this.opCommands =
        new HashMap<>();
      this.commands =
        new TreeMap<>();
    }

    RCCPrimitiveSyncGraph build()
      throws RCCompilerException
    {
      /*
       * For each operation, create accesses. This yields a set of
       * disconnected islands in the graph. The accesses are then
       * linked to connect the islands.
       */

      this.processCreateAccesses();

      /*
       * Now, add barriers for accesses.
       */

      this.processBarriers();

      /*
       * The next pass annotates all operations with the Vulkan queue submission
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
       * The next pass adjusts barriers to turn them into semaphore-blocked
       * queue ownership transfers, where applicable.
       */

      this.processSemaphores();

      for (final var command : this.syncGraph.vertexSet()) {
        this.commands.put(command.commandId(), command);
      }
      return new RCCPrimitiveSyncGraph(this);
    }

    private void processCreateAccesses()
    {
      for (final var op : this.graph.opsOrdered()) {
        this.processCreateAccessesForOp(op);
      }
      for (final var op : this.graph.opsOrdered()) {
        this.processLinkAccessesForOp(op);
      }
      this.processCreateDiscards();
    }

    private void processCreateDiscards()
    {
      final var accesses =
        this.syncGraph.vertexSet()
          .stream()
          .filter(x -> x instanceof RCCAccess)
          .map(RCCAccess.class::cast)
          .sorted(RCCCommandType.idComparator())
          .toList();

      for (final var access : accesses) {
        if (this.syncGraph.outDegreeOf(access) == 0) {
          final var discard =
            new RCCDiscard(
              this.freshId(),
              access.owner(),
              access.resource(),
              access.port()
            );
          traceCreation(discard);
          this.addDependencyBeforeAfter(access, discard);
        }
      }
    }

    private <R extends RCCPPlaceholderType>
    Stream<RCCAccess>
    allAccessesWith(final Class<R> resourceClass)
    {
      return this.syncGraph.vertexSet()
        .stream()
        .filter(c -> c instanceof RCCAccess)
        .map(RCCAccess.class::cast)
        .filter(w -> resourceClass.isAssignableFrom(w.resource().getClass()))
        .sorted(RCCCommandType.idComparator());
    }

    private void processBarriers()
      throws RCCompilerException
    {
      LOG.trace("Processing barriers.");
      this.processBarriersMemory();
      this.processBarriersImage();
      this.checkAccessesHaveBarriers();
      this.checkBarriersAreOptimal();
    }

    private void processBarriersImage()
    {
      final var accesses =
        this.allAccessesWith(RCCPPlaceholderImageType.class)
          .toList();

      for (final var myAccess : accesses) {
        this.processBarriersImageAccess(myAccess);
      }
    }

    private void processBarriersImageAccess(
      final RCCAccess myAccess)
    {
      final var theirCommands =
        this.incomingSyncCommandsOf(myAccess)
          .toList();

      if (theirCommands.isEmpty()) {
        return;
      }

      Invariants.checkInvariantV(
        theirCommands,
        theirCommands.size() == 1,
        "Accesses must have exactly one incoming command."
      );

      final var theirCommand =
        theirCommands.get(0);

      final var transition =
        this.portGraph.imageLayoutTransitionForPort(myAccess.port());

      switch (transition) {
        case RCCPortImageLayout(
          RCCImageLayoutStatusType.Unchanged(
            final RCGResourceImageLayout layoutDuring
          ),
          RCCImageLayoutStatusType.Unchanged(_)
        ) -> {
          switch (theirCommand) {

            /*
             * The command upon which this command depends is an access,
             * so a barrier must be created.
             */

            case final RCCAccess theirAccess -> {
              final var preBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirAccess.writesAt(),
                  myAccess.writesAt(),
                  myAccess.readsAt(),
                  layoutDuring,
                  layoutDuring
                );
              traceCreation(preBarrier);
              preBarrier.setComment(
                "Pre barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.addElementInBetween(theirAccess, myAccess, preBarrier);
            }

            case final RCCImageBarrier _,
                 final RCCBarrierWithQueueTransferType _,
                 final RCCMemoryBarrier _ -> {
              throw new UnreachableCodeException();
            }
          }
        }

        case RCCPortImageLayout(
          RCCImageLayoutStatusType.Changed(
            final RCGResourceImageLayout layoutIncoming,
            final RCGResourceImageLayout layoutDuring
          ),
          RCCImageLayoutStatusType.Unchanged(_)
        ) -> {
          switch (theirCommand) {

            /*
             * The command upon which this command depends is an access,
             * so a barrier must be created.
             */

            case final RCCAccess theirAccess -> {
              final var preBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirAccess.writesAt(),
                  myAccess.writesAt(),
                  myAccess.readsAt(),
                  layoutIncoming,
                  layoutDuring
                );
              traceCreation(preBarrier);
              preBarrier.setComment(
                "Pre barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.addElementInBetween(theirAccess, myAccess, preBarrier);
            }

            /*
             * The command upon which this command depends is a barrier,
             * and we have a layout transition that we need to perform before
             * our accesses can be performed. Therefore, we need a single
             * barrier that encompasses both the other barrier, and a
             * barrier that would perform the layout transition that we
             * need.
             */

            case final RCCImageBarrier theirBarrier -> {
              final var mergedPreBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirBarrier.waitsForWritesAt(),
                  theirBarrier.blocksWritesAt(),
                  theirBarrier.blocksReadsAt(),
                  theirBarrier.layoutFrom(),
                  layoutDuring
                );
              traceCreation(mergedPreBarrier);
              mergedPreBarrier.setComment(
                "Merged barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.replaceBarrier(theirBarrier, mergedPreBarrier);
            }

            case final RCCBarrierWithQueueTransferType _,
                 final RCCMemoryBarrier _ -> {
              throw new UnreachableCodeException();
            }
          }
        }

        case RCCPortImageLayout(
          RCCImageLayoutStatusType.Unchanged(_),
          RCCImageLayoutStatusType.Changed(
            final RCGResourceImageLayout layoutDuring,
            final RCGResourceImageLayout layoutOutgoing
          )
        ) -> {
          switch (theirCommand) {

            /*
             * The command upon which this command depends is an access,
             * so a barrier must be created.
             */

            case final RCCAccess theirAccess -> {
              final var preBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirAccess.writesAt(),
                  myAccess.writesAt(),
                  myAccess.readsAt(),
                  layoutDuring,
                  layoutDuring
                );
              traceCreation(preBarrier);
              preBarrier.setComment(
                "Pre barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.addElementInBetween(theirAccess, myAccess, preBarrier);
              this.insertImageBarrierPost(myAccess, layoutDuring, layoutOutgoing);
            }

            case final RCCImageBarrier _,
                 final RCCBarrierWithQueueTransferType _,
                 final RCCMemoryBarrier _ -> {
              throw new UnreachableCodeException();
            }
          }
        }

        case RCCPortImageLayout(
          RCCImageLayoutStatusType.Changed(
            final RCGResourceImageLayout layoutIncoming,
            final RCGResourceImageLayout layoutDuring
          ),
          RCCImageLayoutStatusType.Changed(
            _,
            final RCGResourceImageLayout layoutOutgoing
          )
        ) -> {
          switch (theirCommand) {

            /*
             * The command upon which this command depends is an access,
             * so a barrier must be created.
             */

            case final RCCAccess theirAccess -> {
              final var preBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirAccess.writesAt(),
                  myAccess.writesAt(),
                  myAccess.readsAt(),
                  layoutIncoming,
                  layoutDuring
                );
              traceCreation(preBarrier);
              preBarrier.setComment(
                "Pre barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.addElementInBetween(theirAccess, myAccess, preBarrier);
              this.insertImageBarrierPost(myAccess, layoutDuring, layoutOutgoing);
            }

            /*
             * The command upon which this command depends is a barrier,
             * and we have a layout transition that we need to perform before
             * our accesses can be performed. Therefore, we need a single
             * barrier that encompasses both the other barrier, and a
             * barrier that would perform the layout transition that we
             * need.
             */

            case final RCCImageBarrier theirBarrier -> {
              final var mergedPreBarrier =
                new RCCImageBarrier(
                  this.freshId(),
                  myAccess.owner(),
                  (RCCPPlaceholderImageType) myAccess.resource(),
                  theirBarrier.waitsForWritesAt(),
                  theirBarrier.blocksWritesAt(),
                  theirBarrier.blocksReadsAt(),
                  theirBarrier.layoutFrom(),
                  layoutDuring
                );
              traceCreation(mergedPreBarrier);
              mergedPreBarrier.setComment(
                "Merged barrier for port %s"
                  .formatted(myAccess.port().fullPath())
              );

              this.replaceBarrier(theirBarrier, mergedPreBarrier);
              this.insertImageBarrierPost(myAccess, layoutDuring, layoutOutgoing);
            }

            case final RCCBarrierWithQueueTransferType _,
                 final RCCMemoryBarrier _ -> {
              throw new UnreachableCodeException();
            }
          }
        }
      }
    }

    private void insertImageBarrierPost(
      final RCCAccess myAccess,
      final RCGResourceImageLayout layoutDuring,
      final RCGResourceImageLayout layoutOutgoing)
    {
      final var nextCommands =
        this.syncGraph.outgoingEdgesOf(myAccess)
          .stream()
          .map(RCCSyncDependency::target)
          .sorted(RCCCommandType.idComparator())
          .toList();

      Invariants.checkInvariantV(
        nextCommands,
        nextCommands.size() <= 1,
        "Must have at most one next command."
      );

      if (nextCommands.isEmpty()) {
        return;
      }

      final var nextCommand = nextCommands.get(0);
      switch (nextCommand) {
        case final RCCAccess nextAccess -> {
          final var postBarrier =
            new RCCImageBarrier(
              this.freshId(),
              myAccess.owner(),
              (RCCPPlaceholderImageType) myAccess.resource(),
              myAccess.writesAt(),
              nextAccess.writesAt(),
              nextAccess.readsAt(),
              layoutDuring,
              layoutOutgoing
            );
          traceCreation(postBarrier);
          postBarrier.setComment(
            "Post barrier for port %s"
              .formatted(myAccess.port().fullPath())
          );

          this.addElementInBetween(myAccess, nextAccess, postBarrier);
        }

        case final RCCBarrierType _ -> {
          throw new UnimplementedCodeException();
        }
        case final RCCMetaCommandType _ -> {
          throw new UnreachableCodeException();
        }
      }
    }

    private void checkAccessesHaveBarriers()
      throws RCCompilerException
    {
      final var edges =
        this.syncGraph.edgeSet()
          .stream()
          .sorted()
          .toList();

      final var exceptions =
        new ExceptionTracker<RCCompilerException>();

      for (final var edge : edges) {
        final var source =
          edge.source();
        final var target =
          edge.target();

        if (source instanceof final RCCAccess sourceAccess
            && target instanceof final RCCAccess targetAccess) {
          final var sourceWrites = !sourceAccess.writesAt().isEmpty();
          final var targetWrites = !targetAccess.writesAt().isEmpty();
          if (sourceWrites || targetWrites) {
            exceptions.addException(errorMissingBarrier(source, target));
          }
        }
      }

      exceptions.throwIfNecessary();
    }

    private void checkBarriersAreOptimal()
      throws RCCompilerException
    {
      final var edges =
        this.syncGraph.edgeSet()
          .stream()
          .sorted()
          .toList();

      final var exceptions =
        new ExceptionTracker<RCCompilerException>();

      for (final var edge : edges) {
        final var source =
          edge.source();
        final var target =
          edge.target();

        if (source instanceof final RCCBarrierType sourceBarrier
            && target instanceof final RCCBarrierType targetBarrier) {
          exceptions.addException(this.errorRedundantBarrier(source, target));
        }
      }

      exceptions.throwIfNecessary();
    }

    private RCCompilerException errorRedundantBarrier(
      final RCCCommandType source,
      final RCCCommandType target)
    {
      LOG.trace("Redundant barriers {} → {}", source, target);

      final var error =
        new SStructuredError<>(
          "error-barrier-redundant",
          "Redundant barriers.",
          Map.ofEntries(
            Map.entry("Source (Kind)", source.kind()),
            Map.entry("Target (Kind)", target.kind()),
            Map.entry("Source (Operation)", source.operationName().value()),
            Map.entry("Target (Operation)", target.operationName().value())
          ),
          Optional.empty(),
          Optional.empty()
        );

      return RCCompilerException.exceptionOf(error);
    }

    private static RCCompilerException errorMissingBarrier(
      final RCCCommandType source,
      final RCCCommandType target)
    {
      LOG.trace("Missing barrier between {} → {}", source, target);

      final var error =
        new SStructuredError<>(
          "error-barrier-missing",
          "Missing barrier.",
          Map.ofEntries(
            Map.entry("Source (Kind)", source.kind()),
            Map.entry("Target (Kind)", target.kind()),
            Map.entry("Source (Operation)", source.operationName().value()),
            Map.entry("Target (Operation)", target.operationName().value())
          ),
          Optional.empty(),
          Optional.empty()
        );

      return RCCompilerException.exceptionOf(error);
    }

    private Stream<RCCAccess> incomingAccessesOf(
      final RCCCommandType command)
    {
      return this.incomingCommandsOf(command)
        .filter(c -> c instanceof RCCAccess)
        .map(RCCAccess.class::cast)
        .sorted(RCCCommandType.idComparator());
    }

    private Stream<RCCSyncCommandType> incomingSyncCommandsOf(
      final RCCCommandType command)
    {
      return this.incomingCommandsOf(command)
        .filter(c -> c instanceof RCCSyncCommandType)
        .map(RCCSyncCommandType.class::cast);
    }

    private Stream<RCCCommandType> incomingCommandsOf(
      final RCCCommandType command)
    {
      return this.syncGraph.incomingEdgesOf(command)
        .stream()
        .map(RCCSyncDependency::source);
    }

    /**
     * Insert barriers for memory accesses. These are barriers that, for a given
     * write `w`, block writes upon which `w` depends.
     */

    private void processBarriersMemory()
    {
      final var access =
        this.allAccessesWith(RCCPPlaceholderMemoryType.class)
          .toList();

      for (final var targetAccess : access) {
        final var incomingWrites =
          this.incomingAccessesOf(targetAccess)
            .toList();

        Invariants.checkInvariantV(
          incomingWrites.size(),
          incomingWrites.size() <= 1,
          "Writes can have at most one incoming write."
        );

        final var writeStages =
          incomingWrites
            .stream()
            .flatMap(c -> c.writesAt().stream())
            .collect(Collectors.toSet());

        /*
         * If the incoming access performs no writes, then no barrier is
         * required.
         */

        if (writeStages.isEmpty()) {
          continue;
        }

        final var barrier =
          new RCCMemoryBarrier(
            this.freshId(),
            targetAccess.owner(),
            (RCCPPlaceholderMemoryType) targetAccess.resource(),
            writeStages,
            targetAccess.writesAt(),
            targetAccess.readsAt()
          );

        barrier.setComment(
          "Barrier for port %s".formatted(targetAccess.port().fullPath())
        );
        traceCreation(barrier);
        for (final var sourceWrite : incomingWrites) {
          this.addElementInBetween(sourceWrite, targetAccess, barrier);
        }
      }
    }

    private void addElementInBetween(
      final RCCCommandType source,
      final RCCCommandType target,
      final RCCCommandType between)
    {
      LOG.trace(
        "Replace {} → {} | {} → {} → {}",
        source,
        target,
        source,
        between,
        target
      );

      this.syncGraph.removeEdge(source, target);
      this.syncGraph.addVertex(between);
      this.syncGraph.addEdge(
        source,
        between,
        new RCCSyncDependency(source, between)
      );
      this.syncGraph.addEdge(
        between,
        target,
        new RCCSyncDependency(between, target)
      );
    }

    private void processLinkAccessesForOp(
      final RCTOperationDeclaration op)
    {
      final var opExec =
        Objects.requireNonNull(this.opCommands.get(op), "opExec");

      /*
       * For each access, find the access that must be connected directly to it.
       */

      for (final var myAccess : opExec.accesses()) {
        final var myPort = myAccess.port();

        /*
         * Producer ports do not have incoming edges.
         */

        if (myPort instanceof RCCPortPrimitiveProducer) {
          continue;
        }

        final var theirPort =
          this.portGraph.graph()
            .incomingEdgesOf(myPort)
            .stream()
            .map(RCCPortPrimitiveConnection::sourcePort)
            .findFirst()
            .orElseThrow(() -> {
              return new IllegalStateException(
                "No port connected to %s".formatted(myPort.fullPath())
              );
            });

        final var theirOp =
          this.opCommands.get(theirPort.owner());

        final var theirAccess =
          theirOp.accesses()
            .stream()
            .filter(w -> Objects.equals(w.port(), theirPort))
            .findFirst()
            .orElseThrow(() -> {
              return new IllegalStateException(
                "No access for port %s".formatted(theirPort.fullPath())
              );
            });

        this.addDependencyBeforeAfter(theirAccess, myAccess);
      }
    }

    private long freshId()
    {
      final var r = this.commandIdNext;
      this.commandIdNext = this.commandIdNext + 1L;
      return r;
    }

    private void addDependencyBeforeAfter(
      final RCCCommandType before,
      final RCCCommandType after)
    {
      this.syncGraph.addVertex(before);
      this.syncGraph.addVertex(after);
      this.syncGraph.addEdge(
        before,
        after,
        new RCCSyncDependency(before, after)
      );

      LOG.trace("Before {} → After {}", before, after);
    }

    private void processSemaphores()
    {
      final var vertices =
        new HashSet<>(this.syncGraph.vertexSet());

      for (final var c : vertices) {
        if (c instanceof final RCCBarrierType barrier) {
          this.processSemaphoreBarrier(barrier);
        }
      }
    }

    private void processSemaphoreBarrier(
      final RCCBarrierType barrier)
    {
      if (barrier instanceof RCCBarrierWithQueueTransferType) {
        return;
      }

      final var targetSubmission =
        barrier.submission();

      final var incomingEdges =
        this.syncGraph.incomingEdgesOf(barrier);
      final var outgoingEdges =
        this.syncGraph.outgoingEdgesOf(barrier);

      Invariants.checkInvariantV(
        barrier,
        !incomingEdges.isEmpty(),
        "Barriers must have incoming edges."
      );
      Invariants.checkInvariantV(
        barrier,
        !outgoingEdges.isEmpty(),
        "Barriers must have outgoing edges."
      );

      for (final var edge : incomingEdges) {
        Invariants.checkInvariantV(
          barrier.equals(edge.target()),
          "Write barrier %s must match the target of edge %s.",
          barrier,
          edge
        );
      }

      /*
       * A barrier becomes a queue transfer if the source of an incoming
       * edge is on a different queue category.
       */

      final var sourceSubmission =
        incomingEdges.iterator().next().source().submission();

      if (Objects.equals(sourceSubmission, targetSubmission)) {
        return;
      }

      final var newBarrier =
        switch (barrier) {
          case final RCCImageBarrier b -> {
            yield new RCCImageBarrierWithQueueTransfer(
              b.commandId(),
              b.owner(),
              b.resource(),
              b.waitsForWritesAt(),
              b.blocksWritesAt(),
              b.blocksReadsAt(),
              b.layoutFrom(),
              b.layoutTo(),
              sourceSubmission,
              targetSubmission
            );
          }

          case final RCCMemoryBarrier b -> {
            yield new RCCMemoryBarrierWithQueueTransfer(
              b.commandId(),
              b.owner(),
              b.resource(),
              b.waitsForWritesAt(),
              b.blocksWritesAt(),
              b.blocksReadsAt(),
              sourceSubmission,
              targetSubmission
            );
          }

          case final RCCBarrierWithQueueTransferType _ -> {
            throw new UnreachableCodeException();
          }
        };

      newBarrier.setSubmission(barrier.submission());
      this.replaceBarrier(barrier, newBarrier);
    }

    private void replaceBarrier(
      final RCCBarrierType oldBarrier,
      final RCCBarrierType newBarrier)
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
      final RCCSyncDependency oldEdge,
      final RCCBarrierType oldBarrier,
      final RCCBarrierType newBarrier)
    {
      if (Objects.equals(oldEdge.source(), oldBarrier)) {
        final var newEdge = new RCCSyncDependency(newBarrier, oldEdge.target());
        LOG.trace("Replace ({}) with ({})", oldEdge, newEdge);
        this.syncGraph.addEdge(newBarrier, oldEdge.target(), newEdge);
      } else if (Objects.equals(oldEdge.target(), oldBarrier)) {
        final var newEdge = new RCCSyncDependency(oldEdge.source(), newBarrier);
        LOG.trace("Replace ({}) with ({})", oldEdge, newEdge);
        this.syncGraph.addEdge(oldEdge.source(), newBarrier, newEdge);
      } else {
        throw new UnreachableCodeException();
      }
    }

    private void processSubmissions()
    {
      /*
       * We need to split the graph up into separate islands based on the
       * queue category of each operation. The islands represent the unique
       * submissions that need to be made to each Vulkan queue.
       */

      final var graphCopy =
        (Graph<RCCCommandType, RCCSyncDependency>) this.syncGraph.clone();

      final var removeEdges =
        new HashSet<RCCSyncDependency>();

      final var edgeSet =
        graphCopy.edgeSet()
          .stream()
          .sorted()
          .toList();

      for (final var edge : edgeSet) {
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

    private void processCreateAccessesForOp(
      final RCTOperationDeclaration op)
    {
      /*
       * Instantiate an Execute command. Make it depend on our accesses.
       */

      final var opCmd = new RCCExecute(this.freshId(), op);
      opCmd.setComment("Command buffers for %s".formatted(op.name()));
      traceCreation(opCmd);
      this.syncGraph.addVertex(opCmd);

      final var opPrimitivePorts =
        this.portGraph.primitivePortsForOp(op.name())
          .stream()
          .sorted(Comparator.comparing(RCCPortPrimitiveType::fullPath))
          .toList();

      for (final var port : opPrimitivePorts) {
        final var resource =
          this.portGraph.resourceForPort(port);

        {
          final var portReads =
            port.reads();
          final var portWrites =
            port.writes();

          if (portReads.isEmpty() && portWrites.isEmpty()) {
            LOG.trace("No reads/writes on port {}", port.fullPath());
            continue;
          }
        }

        final var access =
          opCmd.addAccess(
            this.freshId(),
            resource,
            port.writes(),
            port.reads(),
            port
          );

        final var commentText = new StringBuilder();
        commentText.append("Access on port ");
        commentText.append(port.fullPath());

        if (resource instanceof RCCPPlaceholderImageType) {
          final var requires =
            port.requiresImageLayout();
          requires.ifPresent(layout -> {
            commentText.append(" (Requires ");
            commentText.append(layout);
            commentText.append(")");
          });
        }

        access.setComment(commentText.toString());

        traceCreation(access);
        this.addDependencyBeforeAfter(opCmd, access);

        if (port instanceof final RCCPortPrimitiveProducer producer) {
          switch (resource) {
            case final RCCPPlaceholderImageType image -> {
              final var layout =
                this.portGraph.imageLayoutTransitionForPort(producer);

              final var imageLayout =
                switch (layout.outgoing()) {
                  case final RCCImageLayoutStatusType.Changed changed -> {
                    yield changed.layoutTo();
                  }
                  case final RCCImageLayoutStatusType.Unchanged unchanged -> {
                    yield unchanged.layoutNow();
                  }
                };

              final var i =
                opCmd.addIntroImage(
                  this.freshId(),
                  image,
                  producer,
                  imageLayout
                );

              traceCreation(i);
              this.addDependencyBeforeAfter(i, opCmd);
            }
            case final RCCPPlaceholderMemoryType memory -> {
              final var i =
                opCmd.addIntroMemory(this.freshId(), memory, producer);
              traceCreation(i);
              this.addDependencyBeforeAfter(i, opCmd);
            }
          }
        }
      }

      Preconditions.checkPreconditionV(
        !this.opCommands.containsKey(op),
        "Operations must be processed once."
      );
      this.opCommands.put(op, opCmd);
    }

    private static void traceCreation(
      final RCCCommandType command)
    {
      LOG.trace("Create {} ({})", command, command.comment());
    }

    private Optional<RCCExecute> findIncomingConnectedOp(
      final RCCPortPrimitiveType port)
    {
      final var connectionOpt =
        this.portGraph.graph()
          .incomingEdgesOf(port)
          .stream()
          .findFirst();

      if (connectionOpt.isEmpty()) {
        return Optional.empty();
      }

      final var connection = connectionOpt.get();
      return Optional.of(this.opCommands.get(connection.sourcePort().owner()));
    }
  }
}
