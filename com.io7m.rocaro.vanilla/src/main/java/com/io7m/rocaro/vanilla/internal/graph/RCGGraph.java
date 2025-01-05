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
import com.io7m.rocaro.api.graph.RCGExecutionPlanType;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphOpConnection;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;
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
  private final AsUnmodifiableGraph<RCGPortType<?>, RCGGraphConnection> graphRead;
  private final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph;
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> graph;
  private final List<RCGPortType<?>> portsOrdered;
  private final List<RCGOperationType> opsOrdered;
  private final Map<RCGOperationName, RCGOperationType> ops;
  private final Map<RCGPortType<?>, RCGOperationImageLayoutTransitionType> imageLayouts;
  private final Map<RCGPortType<?>, RCGResourceVariable<?>> portResources;
  private final Map<RCGResourceName, RCGResourceVariable<?>> resources;
  private final RCGraphName name;
  private final VulkanPhysicalDeviceFeatures requiredFeatures;
  private final RCGExecutionPlanType executionPlan;

  /**
   * An immutable compiled graph.
   *
   * @param inName             The graph name
   * @param inGraph            The graph of ports
   * @param inOpGraph          The graph of operations
   * @param inPortsOrdered     The ports in topological order
   * @param inOpsOrdered       The operations in topological order
   * @param inOps              The operations
   * @param inResources        The resources
   * @param inPortResources    The resources at every port
   * @param inImageLayouts     The image layout transitions
   * @param inRequiredFeatures The required device features
   * @param inExecutionPlan    The execution plan
   */

  public RCGGraph(
    final RCGraphName inName,
    final DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> inGraph,
    final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> inOpGraph,
    final List<RCGPortType<?>> inPortsOrdered,
    final List<RCGOperationType> inOpsOrdered,
    final Map<RCGOperationName, RCGOperationType> inOps,
    final Map<RCGResourceName, RCGResourceVariable<?>> inResources,
    final Map<RCGPortType<?>, RCGResourceVariable<?>> inPortResources,
    final Map<RCGPortType<?>, RCGOperationImageLayoutTransitionType> inImageLayouts,
    final VulkanPhysicalDeviceFeatures inRequiredFeatures,
    final RCGExecutionPlanType inExecutionPlan)
  {
    this.name =
      Objects.requireNonNull(inName, "inName");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.opGraph =
      Objects.requireNonNull(inOpGraph, "opGraph");
    this.portsOrdered =
      List.copyOf(inPortsOrdered);
    this.opsOrdered =
      List.copyOf(inOpsOrdered);
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
    this.executionPlan =
      Objects.requireNonNull(inExecutionPlan, "executionPlan");

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
  @SuppressWarnings("unchecked")
  public <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGResourceVariable<S>
  resourceAt(
    final RCGPortType<R> port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.portResources.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return (RCGResourceVariable<S>) r;
  }

  @Override
  public <R extends RCResourceType> RCGOperationImageLayoutTransitionType
  imageTransitionAt(
    final RCGPortType<R> port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.imageLayouts.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return r;
  }

  @Override
  public Graph<RCGPortType<?>, RCGGraphConnection> portGraph()
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

  @Override
  public RCGExecutionPlanType executionPlan()
  {
    return this.executionPlan;
  }
}
