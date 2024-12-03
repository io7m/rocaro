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

import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.assets.RCAssetReferenceType;
import com.io7m.rocaro.api.assets.RCAssetValueFailed;
import com.io7m.rocaro.api.assets.RCAssetValueLoaded;
import com.io7m.rocaro.api.assets.RCAssetValueLoading;
import com.io7m.rocaro.api.graph.RCGFrameScopedServiceType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodePreparationContextType;
import com.io7m.rocaro.api.graph.RCGNodeType;
import com.io7m.rocaro.api.graph.RCGStatusFailed;
import com.io7m.rocaro.api.graph.RCGStatusInProgress;
import com.io7m.rocaro.api.graph.RCGStatusReady;
import com.io7m.rocaro.api.graph.RCGStatusType;
import com.io7m.rocaro.api.graph.RCGStatusUninitialized;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.GRAPH_NOT_READY;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NOT_READY;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;

/**
 * A fully instantiated render graph.
 */

public final class RCGraph
{
  private final RCGraphDescription description;
  private final Map<RCGNodeName, RCGNodeType<?>> nodes;
  private final HashSet<RCGNodeName> nodesEvaluated;
  private final HashMap<RCGNodeName, NodePreparationContext> nodesResources;
  private RCGStatusType status;

  RCGraph(
    final RCGraphDescription inDescription,
    final Map<RCGNodeName, RCGNodeType<?>> inNodes)
  {
    this.description =
      Objects.requireNonNull(inDescription, "description");
    this.nodes =
      Map.copyOf(inNodes);
    this.nodesEvaluated =
      new HashSet<>(this.nodes.size());
    this.nodesResources =
      new HashMap<>();
    this.status =
      new RCGStatusUninitialized(this.description.name());
  }

  @Override
  public String toString()
  {
    return "[RCGraph %s '%s']".formatted(
      Integer.toUnsignedString(this.hashCode(), 16),
      this.description.name()
    );
  }

  /**
   * Evaluate the render graph.
   *
   * @param frameInformation The frame information
   * @param frameContext     The frame context
   * @param strings          The string resources
   *
   * @throws RocaroException On errors
   */

  public void evaluate(
    final RCFrameInformation frameInformation,
    final RCVulkanFrameContextType frameContext,
    final RCStrings strings)
    throws RocaroException
  {
    final var context =
      new RCGNodeRenderContext(
        this.description.graph(),
        frameInformation,
        frameContext
      );

    this.prepareNodes(frameInformation);

    switch (this.status) {
      case final RCGStatusReady _ -> {
        this.evaluateNodes(context);
      }
      case final RCGStatusInProgress _,
           final RCGStatusUninitialized _ -> {
        throw this.errorNotReady(strings);
      }
      case final RCGStatusFailed s -> {
        throw this.errorFailed(strings, s);
      }
    }
  }

  private RocaroException errorFailed(
    final RCStrings strings,
    final RCGStatusFailed failed)
  {
    return new RCGraphDescriptionException(
      strings.format(ERROR_GRAPH_NOT_READY),
      failed.exception(),
      Map.ofEntries(
        Map.entry(strings.format(GRAPH), this.description.name().value())
      ),
      GRAPH_NOT_READY.codeName(),
      Optional.empty()
    );
  }

  private RocaroException errorNotReady(
    final RCStrings strings)
  {
    return new RCGraphDescriptionException(
      strings.format(ERROR_GRAPH_NOT_READY),
      Map.ofEntries(
        Map.entry(strings.format(GRAPH), this.description.name().value())
      ),
      GRAPH_NOT_READY.codeName(),
      Optional.empty()
    );
  }

  /**
   * Prepare the graph.
   *
   * @param frameInformation The current frame information
   *
   * @throws RocaroException On errors
   */

  public void prepare(
    final RCFrameInformation frameInformation)
    throws RocaroException
  {
    this.prepareNodes(frameInformation);
  }

  private void prepareNodes(
    final RCFrameInformation frameInformation)
  {
    /*
     * Bail out immediately if the graph is already ready, or has failed.
     */

    if (this.status instanceof RCGStatusReady
        || this.status instanceof RCGStatusFailed) {
      return;
    }

    /*
     * If this graph has never been prepared, create resource holders for all
     * nodes in the graph.
     */

    if (this.status instanceof RCGStatusUninitialized) {
      for (final var entry : this.nodes.entrySet()) {
        final var name =
          entry.getKey();
        final var node =
          entry.getValue();

        final var context =
          new NodePreparationContext(this, node, frameInformation);

        this.nodesResources.put(name, context);
      }
    }

    /*
     * Prepare all uninitialized nodes, and update the status values of
     * all other nodes.
     */

    for (final var entry : this.nodes.entrySet()) {
      final var name =
        entry.getKey();
      final var context =
        this.nodesResources.get(name);

      context.update();
    }

    /*
     * Check if all the nodes are now ready.
     */

    final var allReady =
      this.nodesResources.values()
        .stream()
        .allMatch(c -> c.status instanceof RCGStatusReady);

    if (allReady) {
      this.status = new RCGStatusReady(this.description.name());
      return;
    }

    /*
     * Check if any nodes have failed.
     */

    final var anyFailed =
      this.nodesResources.values()
        .stream()
        .filter(p -> p.status instanceof RCGStatusFailed)
        .map(p -> p.status)
        .map(RCGStatusFailed.class::cast)
        .findFirst();

    if (anyFailed.isPresent()) {
      this.status = anyFailed.get();
      return;
    }

    /*
     * Check if any nodes are in progress.
     */

    final var anyProgress =
      this.nodesResources.values()
        .stream()
        .filter(p -> p.status instanceof RCGStatusInProgress)
        .map(p -> p.status)
        .map(RCGStatusInProgress.class::cast)
        .findFirst();

    if (anyProgress.isPresent()) {
      this.status = anyProgress.get();
      return;
    }
  }

  private void evaluateNodes(
    final RCGNodeRenderContext context)
    throws RocaroException
  {
    this.nodesEvaluated.clear();

    final var nodeGraph =
      this.description.graph();
    final var iterator =
      new TopologicalOrderIterator<>(nodeGraph);

    while (iterator.hasNext()) {
      final var next = iterator.next();
      final var nodeName = next.owner();
      if (this.nodesEvaluated.contains(nodeName)) {
        continue;
      }

      final var node =
        this.nodes.get(nodeName);
      final var producers =
        node.portProducers();

      node.evaluate(context);

      for (final var producer : producers.values()) {
        if (!context.portIsWritten(producer)) {
          throw new IllegalStateException();
        }
      }

      this.nodesEvaluated.add(nodeName);
    }
  }

  /**
   * @return The status of this graph
   */

  public RCGStatusType status()
  {
    return this.status;
  }

  private static final class NodePreparationContext
    implements RCGNodePreparationContextType
  {
    private final HashMap<Class<? extends RCGFrameScopedServiceType>, RCGFrameScopedServiceType> frameServices;
    private final RCGNodeType<?> node;
    private final RCFrameInformation frameInformation;
    private final HashSet<RCAssetReferenceType<?>> resources;
    private RCGStatusType status;
    private final RCGraph graph;

    public NodePreparationContext(
      final RCGraph inGraph,
      final RCGNodeType<?> inNode,
      final RCFrameInformation inFrameInformation)
    {
      this.graph =
        Objects.requireNonNull(inGraph, "graph");
      this.node =
        Objects.requireNonNull(inNode, "node");
      this.frameInformation =
        Objects.requireNonNull(inFrameInformation, "inFrameInformation");
      this.frameServices =
        new HashMap<>();
      this.resources =
        new HashSet<>(8);
      this.status =
        new RCGStatusUninitialized(inGraph.description.name());
    }

    public RCGStatusType status()
    {
      return this.status;
    }

    @Override
    public RCFrameInformation frameInformation()
    {
      return this.frameInformation;
    }

    @Override
    public <T extends RCGFrameScopedServiceType> T frameScopedService(
      final Class<T> serviceClass)
    {
      return (T) this.frameServices.get(serviceClass);
    }

    public void update()
    {
      switch (this.status) {
        case final RCGStatusReady _,
             final RCGStatusFailed _ -> {
          // Nothing to do.
        }

        case final RCGStatusInProgress _ -> {
          this.updateInProgress();
        }

        case final RCGStatusUninitialized _ -> {
          try {
            this.node.prepare(this);
          } catch (final RocaroException e) {
            this.failed(e);
          }
          this.updateInProgress();
        }
      }
    }

    private void failed(
      final RocaroException e)
    {
      this.status = new RCGStatusFailed(
        this.graph.description.name(),
        e.getMessage(),
        e
      );
    }

    private void updateInProgress()
    {
      var allReady = true;
      var progress = 0.0;

      for (final var r : this.resources) {
        switch (r.get()) {
          case final RCAssetValueFailed<?> s -> {
            this.failed(s.exception());
            return;
          }
          case final RCAssetValueLoaded<?> _ -> {

          }
          case final RCAssetValueLoading<?> s -> {
            allReady = false;
            progress = Math.max(s.progress(), progress);
          }
        }
      }

      if (allReady) {
        this.status = new RCGStatusReady(this.graph.description.name());
      } else {
        this.status = new RCGStatusInProgress(
          this.graph.description.name(),
          "Loading…",
          progress
        );
      }
    }
  }
}
