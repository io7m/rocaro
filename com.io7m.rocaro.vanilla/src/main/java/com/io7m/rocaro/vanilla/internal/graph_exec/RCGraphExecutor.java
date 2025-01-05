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


package com.io7m.rocaro.vanilla.internal.graph_exec;

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RendererGraphProcedureType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGGraphStatusType;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.PreparationFailed;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Preparing;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Ready;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Uninitialized;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGOperationStatusType;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.GRAPH_NOT_READY;
import static com.io7m.rocaro.api.RCStandardErrorCodes.NONEXISTENT_GRAPH;
import static com.io7m.rocaro.api.graph.RCGGraphStatusType.Uninitialized.UNINITIALIZED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NONEXISTENT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NOT_READY;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;

public final class RCGraphExecutor
  extends RCObject
  implements RCGraphExecutorType, RCGOperationPreparationContextType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphExecutor.class);

  private final RCStrings strings;
  private final Map<RCGraphName, RCGGraphType> graphs;
  private final Map<RCGraphName, Map<RCFrameIndex, RCGraphFrameExecutor>> graphFrames;
  private final Map<RCGraphName, RCGGraphStatusType> graphStatus;

  private RCGraphExecutor(
    final RCStrings inStrings,
    final Map<RCGraphName, RCGGraphType> inGraphs)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.graphs =
      Map.copyOf(inGraphs);
    this.graphStatus =
      new HashMap<>();
    this.graphFrames =
      new HashMap<>();
  }

  public static RCGraphExecutorType create(
    final RCStrings strings,
    final Map<RCGraphName, RCGGraphType> graphs,
    final int maximumFrames)
    throws RocaroException
  {
    final var graphExecutor =
      new RCGraphExecutor(strings, graphs);

    for (final var graph : graphs.values()) {
      final var executors =
        new HashMap<RCFrameIndex, RCGraphFrameExecutor>();

      for (var frame = 0; frame < maximumFrames; ++frame) {
        final var frameIndex =
          new RCFrameIndex(frame);
        final var frameExecutor =
          new RCGraphFrameExecutor(graphExecutor, graph, frameIndex);
        executors.put(frameIndex, frameExecutor);
      }
      graphExecutor.graphFrames.put(graph.name(), executors);
    }

    for (final var name : graphExecutor.graphs.keySet()) {
      graphExecutor.updateGraphStatus(name);
    }

    return graphExecutor;
  }

  private boolean prepareGraph(
    final RCGGraphType graph)
    throws RocaroException
  {
    final var graphName =
      graph.name();
    final var order =
      graph.operationExecutionOrder();

    for (final var operation : order) {
      switch (operation.status()) {

        /*
         * If the operation has failed, then the whole graph has failed.
         */

        case final RCGOperationStatusType.PreparationFailed f -> {
          this.updateGraphStatus(graphName);
          throw f.exception();
        }

        /*
         * If the operation is not yet ready, then tell it once again
         * to get ready, and return. An operation is not told to prepare
         * itself until all of its dependencies are ready.
         */

        case final RCGOperationStatusType.Preparing _,
             final RCGOperationStatusType.Uninitialized _ -> {
          operation.prepare(this);
          this.updateGraphStatus(graphName);
          return false;
        }

        case final RCGOperationStatusType.Ready _ -> {
          continue;
        }
      }
    }

    return true;
  }

  @Override
  public void prepare(
    final RCFrameInformation frameInformation,
    final RCGraphName graphName)
    throws RocaroException
  {
    Objects.requireNonNull(frameInformation, "frameInformation");
    Objects.requireNonNull(graphName, "graphName");

    final var status =
      this.graphStatus(graphName);
    final var graph =
      this.graph(graphName);

    switch (status) {

      /*
       * If the graph is ready, or has failed preparation, then return
       * immediately.
       */

      case final PreparationFailed _,
           final Ready _ -> {
        return;
      }

      /*
       * If the graph was preparing last time we checked, or preparation
       * hasn't started, then start iterating through the graph operations
       * and attempting to prepare them.
       */

      case final Preparing _,
           final Uninitialized _ -> {

        if (!this.prepareGraph(graph)) {
          return;
        }
      }
    }

    /*
     * If we've reached this point, then all operations in the graph are
     * ready.
     */

    Postconditions.checkPostconditionV(
      graph.operationExecutionOrder()
        .stream()
        .map(RCGOperationType::status)
        .allMatch(s -> s instanceof RCGOperationStatusType.Ready),
      "All operations must be ready."
    );

    this.updateGraphStatus(graphName);
  }

  @Override
  public RCGGraphStatusType graphStatus(
    final RCGraphName graphName)
  {
    Objects.requireNonNull(graphName, "graphName");
    return this.graphStatus.getOrDefault(graphName, UNINITIALIZED);
  }

  @Override
  public void executeGraph(
    final RCFrameInformation frameInformation,
    final RCGraphName graphName,
    final RCVulkanFrameType frame,
    final RendererGraphProcedureType f)
    throws RocaroException
  {
    Objects.requireNonNull(frameInformation, "frameInformation");
    Objects.requireNonNull(graphName, "graphName");
    Objects.requireNonNull(frame, "frame");
    Objects.requireNonNull(f, "f");

    final var status =
      this.graphStatus.getOrDefault(graphName, UNINITIALIZED);

    switch (status) {
      case final PreparationFailed s -> {
        throw s.exception();
      }

      case final Ready _ -> {
        final var frames =
          this.graphFrames.get(graphName);
        Objects.requireNonNull(frames, "frames");

        final var frameExecutor =
          frames.get(frameInformation.frameIndex());
        Objects.requireNonNull(frameExecutor, "frameExecutor");

        frameExecutor.executeFrame(frameInformation, frame, f);
      }

      case final Preparing _,
           final Uninitialized _ -> {
        throw errorGraphNotReady(this.strings, graphName);
      }
    }
  }

  private RCGGraphType graph(
    final RCGraphName name)
    throws RocaroException
  {
    final var graph = this.graphs.get(name);
    if (graph == null) {
      throw errorNoSuchGraph(this.strings, name);
    }
    return graph;
  }

  private static RocaroException errorNoSuchGraph(
    final RCStrings strings,
    final RCGraphName graphName)
  {
    return new RCGGraphException(
      strings.format(ERROR_GRAPH_NONEXISTENT),
      Map.ofEntries(
        Map.entry(strings.format(GRAPH), graphName.value())
      ),
      NONEXISTENT_GRAPH.codeName(),
      Optional.empty()
    );
  }

  private static RocaroException errorGraphNotReady(
    final RCStrings strings,
    final RCGraphName graphName)
  {
    return new RCGGraphException(
      strings.format(ERROR_GRAPH_NOT_READY),
      Map.ofEntries(
        Map.entry(strings.format(GRAPH), graphName.value())
      ),
      GRAPH_NOT_READY.codeName(),
      Optional.empty()
    );
  }

  private void updateGraphStatus(
    final RCGraphName graphName)
    throws RocaroException
  {
    final var graph =
      this.graph(graphName);
    final var order =
      graph.operationExecutionOrder();

    final var existing =
      this.graphStatus.getOrDefault(graphName, UNINITIALIZED);

    /*
     * If the graph is already ready, we can skip any further checks.
     */

    if (existing instanceof Ready) {
      return;
    }

    /*
     * The graph is ready if every operation in the graph is ready.
     */

    final var allReady =
      order.stream()
        .map(RCGOperationType::status)
        .allMatch(s -> s instanceof RCGOperationStatusType.Ready);

    if (allReady) {
      LOG.trace("Graph '{}' became ready.", graphName);
      this.graphStatus.put(graphName, Ready.READY);
      return;
    }

    /*
     * The graph has failed if any operation in the graph has failed.
     */

    final var anyFailed =
      order.stream()
        .map(RCGOperationType::status)
        .filter(s -> s instanceof RCGOperationStatusType.PreparationFailed)
        .map(RCGOperationStatusType.PreparationFailed.class::cast)
        .findFirst();

    if (anyFailed.isPresent()) {
      LOG.trace("Graph '{}' became failed.", graphName);
      this.graphStatus.put(
        graphName,
        new PreparationFailed(anyFailed.get().exception())
      );
      return;
    }

    /*
     * Otherwise, the progress of the graph is the sum of the progress values
     * of each operation in the graph.
     */

    final var progress =
      order.stream()
        .map(RCGOperationType::status)
        .mapToDouble(RCGOperationStatusType::progress)
        .sum();

    final var message =
      order.stream()
        .map(RCGOperationType::status)
        .filter(s -> s instanceof RCGOperationStatusType.Preparing)
        .map(RCGOperationStatusType.Preparing.class::cast)
        .map(RCGOperationStatusType.Preparing::message)
        .findFirst()
        .orElse("");

    this.graphStatus.put(graphName, new Preparing(message, progress));
  }
}
