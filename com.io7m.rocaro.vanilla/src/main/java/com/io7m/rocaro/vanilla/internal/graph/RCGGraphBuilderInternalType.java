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
import com.io7m.rocaro.api.graph.RCGCommandType;
import com.io7m.rocaro.api.graph.RCGExecutionSubmissionType;
import com.io7m.rocaro.api.graph.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphOpConnection;
import com.io7m.rocaro.api.graph.RCGOperationCreationContextType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourceSubname;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.resources.RCResourceSchematicPrimitiveType;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGGraphPrimitiveConnection;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncDependency;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Consumer;

/**
 * The internal interface exposed by the graph builder.
 */

public interface RCGGraphBuilderInternalType
  extends RCGGraphBuilderType, RCGOperationCreationContextType
{
  /**
   * @return The op graph
   */

  DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph();

  /**
   * @return The commands for each operation
   */

  HashMap<RCGOperationType, List<RCGCommandType>> opCommands();

  /**
   * @return The resource at each port
   */

  HashMap<RCGPortType<?>, RCGResourceVariable<?>> portResourcesTracked();

  /**
   * @return The resource at each primitive port
   */

  HashMap<RCGPortPrimitiveType, RCGResourceVariable<? extends RCResourceSchematicPrimitiveType>> portPrimitiveResourcesTracked();

  /**
   * @return The graph
   */

  DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> graph();

  /**
   * @return The sync_primitive graph
   */

  DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph();

  /**
   * @return The map of operations to their execution sync_primitive commands
   */

  HashMap<RCGOperationType, RCGSExecute> syncOpCommands();

  /**
   * @return The operations
   */

  HashMap<RCGOperationName, RCGOperationType> operations();

  /**
   * @return The resource for each port
   */

  HashMap<RCGPortProducerType<?>, RCGResourceVariable<?>> portResources();

  /**
   * @return The port for each resource
   */

  HashMap<RCGResourceVariable<?>, RCGPortProducerType<?>> resourcePorts();

  /**
   * @return The resources
   */

  HashMap<RCGResourceName, RCGResourceVariable<?>> resources();

  /**
   * @return The image layouts per port
   */

  HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> portImageLayouts();

  /**
   * Traverse the graph in topological order.
   *
   * @param receiver The receiver
   */

  void traverse(Consumer<RCGOperationType> receiver);

  /**
   * Set the required Vulkan device features
   *
   * @param features The features
   */

  void setRequiredFeatures(
    VulkanPhysicalDeviceFeatures features);

  /**
   * Set the list of ports in topological order.
   *
   * @param ports The ports
   */

  void setPortsOrdered(
    List<RCGPortType<?>> ports);

  /**
   * Set the list of operations in topological order.
   *
   * @param operations The operations
   */

  void setOpsOrdered(
    List<RCGOperationType> operations);

  /**
   * @return The list of operations in topological order.
   */

  List<RCGOperationType> opsOrdered();

  /**
   * @return The list of ports in topological order.
   */

  List<RCGPortType<?>> portsOrdered();

  /**
   * Set the submissions for the execution plan.
   *
   * @param submissions The submissions
   */

  void setSubmissions(
    SortedMap<RCGSubmissionID, RCGExecutionSubmissionType> submissions);

  /**
   * @return The graph of ports to primitive ports
   */

  HashMap<RCGPortType<?>, HashMap<RCGResourceSubname, RCGPortPrimitiveType>> portToPrimitives();

  /**
   * @return The primitive port graph
   */

  DirectedAcyclicGraph<
    RCGPortPrimitiveType,
    RCGGraphPrimitiveConnection> primitivePortGraph();

  /**
   * Set the ordered list of primitive ports.
   *
   * @param ports The ports
   */

  void setPrimitivePortsOrdered(
    List<RCGPortPrimitiveType> ports);

  /**
   * @return The list of primitive ports in topological order.
   */

  List<RCGPortPrimitiveType> primitivePortsOrdered();
}
