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


package com.io7m.rocaro.api.graph;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;
import org.jgrapht.Graph;

import java.util.List;

/**
 * A compiled render graph.
 */

public interface RCGGraphType
{
  /**
   * @return The name of the graph
   */

  RCGraphName name();

  /**
   * Obtain the resource for the given port.
   *
   * @param port The port
   * @param <R>  The resource type
   * @param <S>  The resource schematic type
   *
   * @return The resource
   */

  <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGResourceVariable<S> resourceAt(
    RCGPortType<R> port);

  /**
   * Obtain the image layout transition (possibly just a constant value)
   * at the given port. The function returns a constant
   * {@link RCGResourceImageLayout#LAYOUT_UNDEFINED}
   * value for all non-image ports.
   *
   * @param <R>  The resource type
   * @param port The port
   *
   * @return The layout transition
   */

  <R extends RCResourceType>
  RCGOperationImageLayoutTransitionType imageTransitionAt(
    RCGPortType<R> port);

  /**
   * @return A read-only DAG of the ports in the render graph
   */

  Graph<RCGPortType<?>, RCGGraphConnection> portGraph();

  /**
   * @return A read-only DAG of the operations in the render graph
   */

  Graph<RCGOperationType, RCGGraphOpConnection> operationGraph();

  /**
   * @return A read-only view of the operations in execution order
   */

  List<RCGOperationType> operationExecutionOrder();

  /**
   * @return The full set of required device features
   */

  VulkanPhysicalDeviceFeatures requiredDeviceFeatures();

  /**
   * @return The full execution plan
   */

  RCGExecutionPlanType executionPlan();
}
