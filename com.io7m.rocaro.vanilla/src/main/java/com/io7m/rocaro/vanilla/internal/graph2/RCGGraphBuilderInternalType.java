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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.rocaro.api.graph2.RCGCommandType;
import com.io7m.rocaro.api.graph2.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortProduces;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceName;
import com.io7m.rocaro.api.graph2.RCGResourceType;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * The internal interface exposed by the graph builder.
 */

public interface RCGGraphBuilderInternalType
  extends RCGGraphBuilderType
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

  HashMap<RCGPortType, RCGResourceType> portResourcesTracked();

  /**
   * @return The graph
   */

  DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph();

  /**
   * @return The operations
   */

  HashMap<RCGOperationName, RCGOperationType> operations();

  /**
   * @return The operation primitive barriers
   */

  HashMap<RCGOperationName, RCGOperationPrimitiveBarriers> operationPrimitiveBarriers();

  /**
   * @return The resource for each port
   */

  HashMap<RCGPortProduces, RCGResourceType> portResources();

  /**
   * @return The port for each resource
   */

  HashMap<RCGResourceType, RCGPortProduces> resourcePorts();

  /**
   * @return The resources
   */

  HashMap<RCGResourceName, RCGResourceType> resources();

  /**
   * @return The image layouts per port
   */

  HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> portImageLayouts();

  /**
   * Traverse the graph in topological order.
   *
   * @param receiver The receiver
   */

  void traverse(Consumer<RCGOperationType> receiver);
}
