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
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphOpConnection;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;
import com.io7m.rocaro.api.graph.RCGraphName;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable compiled graph.
 */

public final class RCGGraph implements RCGGraphType
{
  private final AsUnmodifiableGraph<RCGOperationType, RCGGraphOpConnection> opGraphRead;
  private final AsUnmodifiableGraph<RCGPortType, RCGGraphConnection> graphRead;
  private final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph;
  private final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph;
  private final List<RCGPortType> portsOrdered;
  private final List<RCGOperationType> opsOrdered;
  private final Map<RCGOperationName, RCGOperationType> ops;
  private final Map<RCGPortType, RCGOperationImageLayoutTransitionType> imageLayouts;
  private final Map<RCGPortType, RCGResourcePlaceholderType> portResources;
  private final Map<RCGResourceName, RCGResourcePlaceholderType> resources;
  private final RCGraphName name;
  private final VulkanPhysicalDeviceFeatures requiredFeatures;

  /**
   * An immutable compiled graph.
   *
   * @param inName             The graph name
   * @param inGraph            The graph of ports
   * @param inOpGraph          The graph of operations
   * @param portsOrdered       The ports in topological order
   * @param opsOrdered         The operations in topological order
   * @param inOps              The operations
   * @param inResources        The resources
   * @param inPortResources    The resources at every port
   * @param inImageLayouts     The image layout transitions
   * @param inRequiredFeatures The required device features
   */

  public RCGGraph(
    final RCGraphName inName,
    final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> inGraph,
    final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> inOpGraph,
    final List<RCGPortType> portsOrdered,
    final List<RCGOperationType> opsOrdered,
    final Map<RCGOperationName, RCGOperationType> inOps,
    final Map<RCGResourceName, RCGResourcePlaceholderType> inResources,
    final Map<RCGPortType, RCGResourcePlaceholderType> inPortResources,
    final Map<RCGPortType, RCGOperationImageLayoutTransitionType> inImageLayouts,
    final VulkanPhysicalDeviceFeatures inRequiredFeatures)
  {
    this.name =
      Objects.requireNonNull(inName, "inName");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.opGraph =
      Objects.requireNonNull(inOpGraph, "opGraph");
    this.portsOrdered =
      List.copyOf(portsOrdered);
    this.opsOrdered =
      List.copyOf(opsOrdered);
    this.ops =
      Objects.requireNonNull(inOps, "ops");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.portResources =
      Objects.requireNonNull(inPortResources, "portResources");
    this.imageLayouts =
      Objects.requireNonNull(inImageLayouts, "imageLayouts");
    this.requiredFeatures =
      Objects.requireNonNull(inRequiredFeatures, "requiredFeatures");

    this.graphRead =
      new AsUnmodifiableGraph<>(this.graph);
    this.opGraphRead =
      new AsUnmodifiableGraph<>(this.opGraph);
  }

  @Override
  public RCGraphName name()
  {
    return this.name;
  }

  @Override
  public RCGResourcePlaceholderType resourceAt(
    final RCGPortType port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.portResources.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return r;
  }

  @Override
  public RCGOperationImageLayoutTransitionType imageTransitionAt(
    final RCGPortType port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.imageLayouts.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return r;
  }

  @Override
  public Graph<RCGPortType, RCGGraphConnection> portGraph()
  {
    return this.graphRead;
  }

  @Override
  public Graph<RCGOperationType, RCGGraphOpConnection> operationGraph()
  {
    return this.opGraphRead;
  }

  @Override
  public List<RCGOperationType> operationExecutionOrder()
  {
    return this.opsOrdered;
  }

  @Override
  public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return this.requiredFeatures;
  }
}
