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


package com.io7m.rocaro.vanilla.internal.graph.plan;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.rocaro.api.graph.RCGBarrierBufferType;
import com.io7m.rocaro.api.graph.RCGBarrierImageType;
import com.io7m.rocaro.api.graph.RCGBarrierType;
import com.io7m.rocaro.api.graph.RCGExecuteOperation;
import com.io7m.rocaro.api.graph.RCGExecutionBarrierSet;
import com.io7m.rocaro.api.graph.RCGExecutionItemType;
import com.io7m.rocaro.api.graph.RCGExecutionSubmissionType;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGMemoryDependencyRead;
import com.io7m.rocaro.api.graph.RCGMemoryDependencyType;
import com.io7m.rocaro.api.graph.RCGMemoryDependencyWrite;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGQueueTransferBufferType;
import com.io7m.rocaro.api.graph.RCGQueueTransferImageType;
import com.io7m.rocaro.api.graph.RCGQueueTransferType;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGSemaphoreBinaryType;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImageType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphPassType;
import com.io7m.rocaro.vanilla.internal.graph.RCGPassAbstract;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPassPortPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGPassSyncPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSBarrierType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSBarrierWithQueueTransferType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSReadBarrierType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSReadType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSWriteBarrierType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSWriteType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncDependency;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.MaskSubgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A pass that produces a final execution plan for a graph.
 */

public final class RCGPassPlan
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  private final TreeMap<RCGSubmissionID, RCGSubmission> submissions;
  private DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph;

  /**
   * A pass that produces a final execution plan for a graph.
   */

  public RCGPassPlan()
  {
    super(Set.of(
      RCGPassPortPrimitive.class,
      RCGPassSyncPrimitive.class
    ));

    this.submissions = new TreeMap<>();
  }

  private static boolean shouldAppearInSubgraph(
    final RCGSExecute execution,
    final RCGSyncCommandType command)
  {
    return !switch (command) {
      case final RCGSBarrierType b -> {
        yield (Objects.equals(b.owner(), execution));
      }
      case final RCGSExecute e -> {
        yield (e.equals(execution));
      }
      case final RCGSReadType r -> {
        yield (Objects.equals(r.owner(), execution));
      }
      case final RCGSWriteType w -> {
        yield (Objects.equals(w.owner(), execution));
      }
    };
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    this.syncGraph =
      builder.syncGraph();
    final var syncCommands =
      builder.syncOpCommands();
    final var ordered =
      builder.opsOrdered();

    /*
     * Initialize all required submissions. This work is done up-front because
     * some barriers will need to insert semaphores into other submissions, so
     * those submissions need to actually exist first.
     */

    for (final var op : ordered) {
      this.initializeSubmission(syncCommands.get(op));
    }

    for (final var op : ordered) {
      this.processExecution(syncCommands.get(op));
    }

    for (final var submission : this.submissions.values()) {
      submission.finish();
    }

    builder.setSubmissions(
      Collections.unmodifiableSortedMap(this.submissions)
    );
  }

  private void initializeSubmission(
    final RCGSExecute execution)
  {
    this.submissions.computeIfAbsent(
      execution.submission(),
      _ -> new RCGSubmission(execution.submission())
    );
  }

  private void processExecution(
    final RCGSExecute execution)
  {
    Objects.requireNonNull(execution, "execution");

    final var subgraph =
      new MaskSubgraph<>(
        this.syncGraph,
        command -> shouldAppearInSubgraph(execution, command),
        _ -> false
      );

    final var submission = this.submissions.get(execution.submission());
    this.processExecutionQueueTransfers(subgraph, submission, execution);
    this.processExecutionReads(subgraph, submission, execution);
    this.processExecutionWrites(subgraph, submission, execution);
    submission.addExecution(execution);
  }

  private void processExecutionQueueTransfers(
    final MaskSubgraph<RCGSyncCommandType, RCGSyncDependency> subgraph,
    final RCGSubmission submission,
    final RCGSExecute execution)
  {
    /*
     * A barrier with a queue transfer actually needs to be executed
     * in multiple queues. However, at run-time, there might not actually
     * be separate queues. We create a split barrier that gives the
     * full details of the queue transfer so that implementations that
     * only have a single queue can opt to use a simple barrier instead
     * of multiple barriers and semaphore.
     */

    final var queueTransfers =
      subgraph.vertexSet()
        .stream()
        .filter(v -> v instanceof RCGSBarrierWithQueueTransferType)
        .map(RCGSBarrierWithQueueTransferType.class::cast)
        .collect(Collectors.toSet());

    final var resourceTransfers =
      new HashMap<RCGResourceVariable<?>, RCGQueueTransferType>();

    for (final var barrier : queueTransfers) {

      /*
       * We know that queue transfers mark both the end of and start of
       * a submission. There are, therefore, no incoming edges in this
       * submission. Additionally, one half of the created barrier will be
       * the last execution item in the other submission.
       */

      Invariants.checkInvariantV(
        subgraph.incomingEdgesOf(barrier).isEmpty(),
        "Queue transfers do not have incoming edges in subgraphs."
      );

      final var resource =
        barrier.resource();

      final var transfer =
        switch (barrier) {
          case final RCGSImageReadBarrierWithQueueTransfer _,
               final RCGSImageWriteBarrierWithQueueTransfer _ -> {

            Invariants.checkInvariantV(
              resource.schematic(),
              resource.schematic() instanceof RCResourceSchematicImageType,
              "Schematic must be of an image type."
            );

            yield resourceTransfers.computeIfAbsent(
              resource, _ -> {
                return new RCGQueueTransferBarrierImage(
                  resource,
                  barrier.queueSource(),
                  barrier.queueTarget()
                );
              }
            );
          }
          case final RCGSMemoryReadBarrierWithQueueTransfer _,
               final RCGSMemoryWriteBarrierWithQueueTransfer _ -> {

            Invariants.checkInvariantV(
              resource.schematic(),
              resource.schematic() instanceof RCResourceSchematicBufferType,
              "Schematic must be of a buffer type."
            );

            yield resourceTransfers.computeIfAbsent(
              resource, _ -> {
                return new RCGQueueTransferBarrierBuffer(
                  resource,
                  barrier.queueSource(),
                  barrier.queueTarget()
                );
              }
            );
          }
        };

      switch (barrier) {
        case final RCGSImageReadBarrierWithQueueTransfer b -> {
          final var tt = (RCGQueueTransferBarrierImage) transfer;
          tt.addDependency(
            new RCGMemoryDependencyRead(b.waitsForWriteAt(), b.blocksReadAt())
          );
        }
        case final RCGSImageWriteBarrierWithQueueTransfer b -> {
          final var tt = (RCGQueueTransferBarrierImage) transfer;
          tt.addDependency(
            new RCGMemoryDependencyWrite(b.waitsForWriteAt(), b.blocksWriteAt())
          );
        }
        case final RCGSMemoryReadBarrierWithQueueTransfer b -> {
          final var tt = (RCGQueueTransferBarrierBuffer) transfer;
          tt.addDependency(
            new RCGMemoryDependencyRead(b.waitsForWriteAt(), b.blocksReadAt())
          );
        }
        case final RCGSMemoryWriteBarrierWithQueueTransfer b -> {
          final var tt = (RCGQueueTransferBarrierBuffer) transfer;
          tt.addDependency(
            new RCGMemoryDependencyWrite(b.waitsForWriteAt(), b.blocksWriteAt())
          );
        }
      }
    }

    for (final var transfer : resourceTransfers.values()) {
      Invariants.checkInvariantV(
        Objects.equals(submission.submissionId(), transfer.queueTarget()),
        "Transfer target must match this submission."
      );
      Invariants.checkInvariantV(
        !Objects.equals(submission.submissionId(), transfer.queueSource()),
        "Transfer source must not match this submission."
      );

      final var otherSubmission =
        this.submissions.get(transfer.queueSource());

      otherSubmission.addEndingQueueTransfer(transfer);
      submission.addStartingQueueTransfer(transfer);
    }
  }

  private void processExecutionReads(
    final MaskSubgraph<RCGSyncCommandType, RCGSyncDependency> subgraph,
    final RCGSubmission submission,
    final RCGSExecute execution)
  {
    final var resourceBarriers =
      new HashMap<RCGResourceVariable<?>, RCGBarrier>();

    final var readBarriers =
      subgraph.vertexSet()
        .stream()
        .filter(v -> v instanceof RCGSReadBarrierType)
        .map(RCGSReadBarrierType.class::cast)
        .collect(Collectors.toSet());

    final var subId = submission.submissionId();
    for (final var readBarrier : readBarriers) {
      final var resource =
        readBarrier.resource();

      final var name =
        "ReadBarrier-[%s:%s]".formatted(
          execution.operation().name().value(),
          resource.name().value()
        );

      switch (readBarrier) {
        case final RCGSImageReadBarrier b -> {
          Invariants.checkInvariantV(
            resource.schematic(),
            resource.schematic() instanceof RCResourceSchematicImageType,
            "Schematic must be of an image type."
          );

          final var imageR =
            (RCGResourceVariable<? extends RCResourceSchematicImageType>)
              resource;

          final var barrier =
            resourceBarriers.computeIfAbsent(
              resource,
              _ -> {
                return new RCGBarrierImage(
                  name,
                  subId,
                  new HashSet<>(),
                  imageR
                );
              }
            );

          barrier.dependencies.add(
            new RCGMemoryDependencyRead(b.waitsForWriteAt(), b.blocksReadAt())
          );
        }

        case final RCGSMemoryReadBarrier b -> {
          Invariants.checkInvariantV(
            resource.schematic(),
            resource.schematic() instanceof RCResourceSchematicBufferType,
            "Schematic must be of a buffer type."
          );

          final var bufferR =
            (RCGResourceVariable<? extends RCResourceSchematicBufferType>)
              resource;

          final var barrier =
            resourceBarriers.computeIfAbsent(
              resource,
              _ -> {
                return new RCGBarrierBuffer(
                  name,
                  subId,
                  new HashSet<>(),
                  bufferR
                );
              }
            );

          barrier.dependencies.add(
            new RCGMemoryDependencyRead(b.waitsForWriteAt(), b.blocksReadAt())
          );
        }

        case final RCGSImageReadBarrierWithQueueTransfer _,
             final RCGSMemoryReadBarrierWithQueueTransfer _ -> {
          // Already processed.
        }
      }
    }

    for (final var barrier : resourceBarriers.values()) {
      submission.addBarrier(barrier);
    }
  }

  private void processExecutionWrites(
    final MaskSubgraph<RCGSyncCommandType, RCGSyncDependency> subgraph,
    final RCGSubmission submission,
    final RCGSExecute execution)
  {
    final var resourceBarriers =
      new HashMap<RCGResourceVariable<?>, RCGBarrier>();

    final var writeBarriers =
      subgraph.vertexSet()
        .stream()
        .filter(v -> v instanceof RCGSWriteBarrierType)
        .map(RCGSWriteBarrierType.class::cast)
        .collect(Collectors.toSet());

    final var subId = submission.submissionId();
    for (final var writeBarrier : writeBarriers) {
      final var resource =
        writeBarrier.resource();

      final var name =
        "WriteBarrier-[%s:%s]".formatted(
          execution.operation().name().value(),
          resource.name().value()
        );

      switch (writeBarrier) {
        case final RCGSImageWriteBarrier b -> {
          final var imageR =
            (RCGResourceVariable<? extends RCResourceSchematicImage2DType>)
              resource;

          final var barrier =
            resourceBarriers.computeIfAbsent(
              resource,
              _ -> {
                return new RCGBarrierImage(
                  name,
                  subId,
                  new HashSet<>(),
                  imageR
                );
              }
            );

          barrier.dependencies.add(
            new RCGMemoryDependencyWrite(b.waitsForWriteAt(), b.blocksWriteAt())
          );
        }

        case final RCGSMemoryWriteBarrier b -> {
          final var bufferR =
            (RCGResourceVariable<? extends RCResourceSchematicBufferType>)
              resource;

          final var barrier =
            resourceBarriers.computeIfAbsent(
              resource,
              _ -> {
                return new RCGBarrierBuffer(
                  name,
                  subId,
                  new HashSet<>(),
                  bufferR
                );
              }
            );

          barrier.dependencies.add(
            new RCGMemoryDependencyWrite(b.waitsForWriteAt(), b.blocksWriteAt())
          );
        }

        case final RCGSImageWriteBarrierWithQueueTransfer _,
             final RCGSMemoryWriteBarrierWithQueueTransfer _ -> {
          // Already processed
        }
      }
    }

    for (final var barrier : resourceBarriers.values()) {
      submission.addBarrier(barrier);
    }
  }

  private static final class RCGQueueTransferBarrierBuffer
    extends RCGQueueTransferBarrier
    implements RCGQueueTransferBufferType
  {
    RCGQueueTransferBarrierBuffer(
      final RCGResourceVariable<?> inResource,
      final RCGSubmissionID inQueueSource,
      final RCGSubmissionID inQueueTarget)
    {
      super(inResource, inQueueSource, inQueueTarget);
    }

    @Override
    public RCGResourceVariable<? extends RCResourceSchematicBufferType> buffer()
    {
      return (RCGResourceVariable<? extends RCResourceSchematicBufferType>)
        this.resource();
    }
  }

  private static final class RCGQueueTransferBarrierImage
    extends RCGQueueTransferBarrier
    implements RCGQueueTransferImageType
  {
    RCGQueueTransferBarrierImage(
      final RCGResourceVariable<?> inResource,
      final RCGSubmissionID inQueueSource,
      final RCGSubmissionID inQueueTarget)
    {
      super(inResource, inQueueSource, inQueueTarget);
    }

    @Override
    public RCGResourceVariable<? extends RCResourceSchematicImage2DType> image()
    {
      return (RCGResourceVariable<? extends RCResourceSchematicImage2DType>)
        this.resource();
    }
  }

  private static abstract class RCGQueueTransferBarrier
  {
    private final RCGResourceVariable<?> resource;
    private final RCGSubmissionID queueSource;
    private final RCGSubmissionID queueTarget;
    private final Set<RCGMemoryDependencyType> dependencies;
    private final Set<RCGMemoryDependencyType> dependenciesRead;

    RCGQueueTransferBarrier(
      final RCGResourceVariable<?> inResource,
      final RCGSubmissionID inQueueSource,
      final RCGSubmissionID inQueueTarget)
    {
      this.resource =
        Objects.requireNonNull(inResource, "resource");
      this.queueSource =
        Objects.requireNonNull(inQueueSource, "queueSource");
      this.queueTarget =
        Objects.requireNonNull(inQueueTarget, "queueTarget");
      this.dependencies =
        new HashSet<>();
      this.dependenciesRead =
        Collections.unmodifiableSet(this.dependencies);
    }

    protected final RCGResourceVariable<?> resource()
    {
      return this.resource;
    }

    public final Set<RCGMemoryDependencyType> dependencies()
    {
      return this.dependenciesRead;
    }

    public final RCGSubmissionID queueSource()
    {
      return this.queueSource;
    }

    public final RCGSubmissionID queueTarget()
    {
      return this.queueTarget;
    }

    public void addDependency(
      final RCGMemoryDependencyType dependency)
    {
      this.dependencies.add(
        Objects.requireNonNull(dependency, "dependency")
      );
    }
  }

  private static final class RCGSubmission
    implements RCGExecutionSubmissionType
  {
    private final RCGSubmissionID submissionId;
    private final List<RCGExecutionItemType> items;
    private final ArrayList<RCGBarrierType> barrierSet;
    private final HashSet<RCGSemaphoreBinaryType> waitSemaphores;
    private final HashSet<RCGSemaphoreBinaryType> signalSemaphores;
    private final List<RCGExecutionItemType> itemsRead;
    private final Set<RCGSemaphoreBinaryType> waitSemaphoresRead;
    private final Set<RCGSemaphoreBinaryType> signalSemaphoresRead;
    private final ArrayList<RCGQueueTransferType> startingQueueTransfers;
    private final ArrayList<RCGQueueTransferType> endingQueueTransfers;
    private final List<RCGQueueTransferType> startingQueueTransfersRead;
    private final List<RCGQueueTransferType> endingQueueTransfersRead;

    public RCGSubmission(
      final RCGSubmissionID inSubmissionId)
    {
      this.submissionId =
        Objects.requireNonNull(inSubmissionId, "submissionId");
      this.items =
        new ArrayList<>();
      this.barrierSet =
        new ArrayList<>();
      this.waitSemaphores =
        new HashSet<>();
      this.signalSemaphores =
        new HashSet<>();
      this.startingQueueTransfers =
        new ArrayList<>();
      this.endingQueueTransfers =
        new ArrayList<>();

      this.itemsRead =
        Collections.unmodifiableList(this.items);
      this.waitSemaphoresRead =
        Collections.unmodifiableSet(this.waitSemaphores);
      this.signalSemaphoresRead =
        Collections.unmodifiableSet(this.signalSemaphores);
      this.startingQueueTransfersRead =
        Collections.unmodifiableList(this.startingQueueTransfers);
      this.endingQueueTransfersRead =
        Collections.unmodifiableList(this.endingQueueTransfers);
    }

    @Override
    public RCGSubmissionID submissionId()
    {
      return this.submissionId;
    }

    @Override
    public List<RCGExecutionItemType> items()
    {
      return this.itemsRead;
    }

    @Override
    public Set<RCGSemaphoreBinaryType> waitSemaphores()
    {
      return this.waitSemaphoresRead;
    }

    @Override
    public Set<RCGSemaphoreBinaryType> signalSemaphores()
    {
      return this.signalSemaphoresRead;
    }

    @Override
    public List<RCGQueueTransferType> startingQueueTransfers()
    {
      return this.startingQueueTransfersRead;
    }

    @Override
    public List<RCGQueueTransferType> endingQueueTransfers()
    {
      return this.endingQueueTransfersRead;
    }

    public void addExecution(
      final RCGSExecute execution)
    {
      this.finishBarrierSet();

      this.items.add(
        new RCGExecuteOperation(
          this.submissionId,
          execution.operation()
        )
      );
    }

    private void finishBarrierSet()
    {
      if (!this.barrierSet.isEmpty()) {
        this.items.add(
          new RCGExecutionBarrierSet(
            "BarrierSet",
            this.submissionId(),
            List.copyOf(this.barrierSet)
          )
        );
        this.barrierSet.clear();
      }
    }

    public void addBarrier(
      final RCGBarrier barrier)
    {
      switch (barrier) {
        case final RCGBarrierBuffer b -> {
          this.barrierSet.add(b);
        }
        case final RCGBarrierImage b -> {
          this.barrierSet.add(b);
        }
      }
    }

    public void finish()
    {
      this.finishBarrierSet();
    }

    public void addStartingQueueTransfer(
      final RCGQueueTransferType transfer)
    {
      this.startingQueueTransfers.add(transfer);
    }

    public void addEndingQueueTransfer(
      final RCGQueueTransferType transfer)
    {
      this.endingQueueTransfers.add(transfer);
    }
  }

  private static sealed abstract class RCGBarrier
  {
    private final RCGSubmissionID submission;
    private final Set<RCGMemoryDependencyType> dependencies;
    private final String name;

    RCGBarrier(
      final String inName,
      final RCGSubmissionID inSubmission,
      final Set<RCGMemoryDependencyType> inDependencies)
    {
      this.name =
        Objects.requireNonNull(inName, "name");
      this.submission =
        Objects.requireNonNull(inSubmission, "submission");
      this.dependencies =
        Objects.requireNonNull(inDependencies, "inDependencies");
    }

    @Override
    public String toString()
    {
      return "[%s '%s' %s %s]"
        .formatted(
          this.getClass().getSimpleName(),
          this.name(),
          this.submission(),
          this.dependencies()
        );
    }

    public final String name()
    {
      return this.name;
    }

    public final RCGSubmissionID submission()
    {
      return this.submission;
    }

    public final Set<RCGMemoryDependencyType> dependencies()
    {
      return Set.copyOf(this.dependencies);
    }
  }

  private static final class RCGBarrierBuffer
    extends RCGBarrier
    implements RCGBarrierBufferType
  {
    private final RCGResourceVariable<? extends RCResourceSchematicBufferType> resource;

    RCGBarrierBuffer(
      final String name,
      final RCGSubmissionID submission,
      final Set<RCGMemoryDependencyType> dependencies,
      final RCGResourceVariable<? extends RCResourceSchematicBufferType> inResource)
    {
      super(name, submission, dependencies);
      this.resource =
        Objects.requireNonNull(inResource, "resource");
    }

    @Override
    public RCGResourceVariable<? extends RCResourceSchematicBufferType> buffer()
    {
      return this.resource;
    }
  }

  private static final class RCGBarrierImage
    extends RCGBarrier
    implements RCGBarrierImageType
  {
    private final RCGResourceVariable<? extends RCResourceSchematicImageType> resource;

    RCGBarrierImage(
      final String name,
      final RCGSubmissionID submission,
      final Set<RCGMemoryDependencyType> dependencies,
      final RCGResourceVariable<? extends RCResourceSchematicImageType> inResource)
    {
      super(name, submission, dependencies);
      this.resource =
        Objects.requireNonNull(inResource, "resource");
    }

    @Override
    public RCGResourceVariable<? extends RCResourceSchematicImageType> image()
    {
      return this.resource;
    }
  }
}
