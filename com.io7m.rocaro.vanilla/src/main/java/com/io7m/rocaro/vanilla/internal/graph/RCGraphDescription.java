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

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.rocaro.api.graph.RCGNodeDescriptionType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodeType;
import com.io7m.rocaro.api.graph.RCGPortConnection;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGraphName;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable description of a render graph.
 */

public final class RCGraphDescription
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphDescription.class);

  private final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph;
  private final Map<RCGNodeName, RCGNodeDescriptionType<?, ?>> graphNodeDescriptions;
  private final VulkanPhysicalDeviceFeatures requiredDeviceFeatures;
  private final RCGraphName name;

  RCGraphDescription(
    final RCGraphName inName,
    final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> inGraph,
    final Map<RCGNodeName, RCGNodeDescriptionType<?, ?>> inGraphNodeDescriptions,
    final VulkanPhysicalDeviceFeatures inRequiredFeatures)
  {
    this.name =
      Objects.requireNonNull(inName, "inName");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.graphNodeDescriptions =
      Objects.requireNonNull(inGraphNodeDescriptions, "graphNodes");
    this.requiredDeviceFeatures =
      Objects.requireNonNull(inRequiredFeatures, "requiredFeatures");
  }

  /**
   * @return The graph
   */

  public DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph()
  {
    return this.graph;
  }

  /**
   * @return The full set of required device features
   */

  public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return this.requiredDeviceFeatures;
  }

  /**
   * @return The graph node descriptions
   */

  public Map<RCGNodeName, RCGNodeDescriptionType<?, ?>> graphNodeDescriptions()
  {
    return this.graphNodeDescriptions;
  }

  /**
   * @return The graph name
   */

  public RCGraphName name()
  {
    return this.name;
  }

  /**
   * Instantiate all nodes in the graph.
   *
   * @return The instantiated graph
   */

  public RCGraph instantiate()
  {
    final var nodes =
      new HashMap<RCGNodeName, RCGNodeType<?>>(
        this.graphNodeDescriptions.size()
      );

    if (this.graphNodeDescriptions.isEmpty()) {
      LOG.warn("Graph '{}' is empty. This is unlikely to work.", this.name);
    }

    for (final var entry : this.graphNodeDescriptions.entrySet()) {
      final var nodeName =
        entry.getKey();
      final var nodeDescription =
        entry.getValue();
      final var node =
        nodeDescription.createNode();

      Objects.requireNonNull(node, "node");
      LOG.debug("Instantiate {} -> {}", nodeName, node);

      nodes.put(nodeName, node);
    }

    return new RCGraph(this, nodes);
  }
}
