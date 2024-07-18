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
import com.io7m.rocaro.api.graph.RCGPortConnection;
import com.io7m.rocaro.api.graph.RCGPortType;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable description of a render graph.
 */

public final class RCGraphDescription
{
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph;
  private final Map<RCGNodeName, RCGNodeDescriptionType<?>> graphNodes;
  private final VulkanPhysicalDeviceFeatures requiredDeviceFeatures;

  RCGraphDescription(
    final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> inGraph,
    final Map<RCGNodeName, RCGNodeDescriptionType<?>> inGraphNodes,
    final VulkanPhysicalDeviceFeatures inRequiredFeatures)
  {
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.graphNodes =
      Objects.requireNonNull(inGraphNodes, "graphNodes");
    this.requiredDeviceFeatures =
      Objects.requireNonNull(inRequiredFeatures, "requiredFeatures");
  }

  /**
   * @return The graph
   */

  public DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph()
  {
    return graph;
  }

  /**
   * @return The full set of required device features
   */

  public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return requiredDeviceFeatures;
  }

  /**
   * @return The graph nodes
   */

  public Map<RCGNodeName, RCGNodeDescriptionType<?>> graphNodes()
  {
    return graphNodes;
  }
}
