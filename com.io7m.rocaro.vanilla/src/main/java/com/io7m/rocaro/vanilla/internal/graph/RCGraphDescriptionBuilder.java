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
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures10;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures11;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures12;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures13;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.rocaro.api.graph.RCGNodeDescriptionType;
import com.io7m.rocaro.api.graph.RCGNodeFactoryType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodeType;
import com.io7m.rocaro.api.graph.RCGPortConnection;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortTargetType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionBuilderType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.images.RCImageColorFormat;
import com.io7m.rocaro.api.images.RCImageDepthFormatType;
import com.io7m.rocaro.api.images.RCImageDescriptionType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_NODE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_PORT_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORTS_INCOMPATIBLE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_CYCLIC_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_NOT_CONNECTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NODE_NAME_ALREADY_USED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_CONSTRAINT_ERROR;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_CYCLIC;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_DUPLICATE_TARGET;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_NOT_CONNECTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_PORT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_PORT_PROVIDES;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_PORT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_PORT_REQUIRES;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TYPE;
import static com.io7m.rocaro.vanilla.internal.images.RCImageColor.IMAGE_COLOR;
import static com.io7m.rocaro.vanilla.internal.images.RCImageDepth.IMAGE_DEPTH;

/**
 * A render graph builder.
 */

public final class RCGraphDescriptionBuilder
  implements RCGraphDescriptionBuilderType
{
  private final RCStrings strings;
  private final String graphName;
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph;
  private final HashMap<RCGNodeName, RCGNodeDescriptionType<?>> graphNodes;

  /**
   * A render graph builder.
   *
   * @param inStrings The string resources
   * @param inName    The graph name
   */

  public RCGraphDescriptionBuilder(
    final RCStrings inStrings,
    final String inName)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.graphName =
      Objects.requireNonNull(inName, "name");
    this.graphNodes =
      new HashMap<>();
    this.graph =
      new DirectedAcyclicGraph<>(RCGPortConnection.class);
  }

  @Override
  public <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P>> D declare(
    final RCGNodeName name,
    final P parameters,
    final RCGNodeFactoryType<P, N, D> nodeFactory)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(nodeFactory, "nodeFactory");

    if (this.graphNodes.containsKey(name)) {
      throw this.errorGraphNodeExists(name, nodeFactory.type());
    }

    final var node = nodeFactory.createDescription(parameters, name);
    Objects.requireNonNull(node, "node");

    this.graphNodes.put(name, node);
    for (final var port : node.ports().values()) {
      this.graph.addVertex(port);
    }

    return node;
  }

  @Override
  public RCImageDescriptionType<RCImageColorFormat> declareColorImage(
    final RCGNodeName name,
    final RCImageColorFormat format)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(format, "format");

    return this.declare(name, format, IMAGE_COLOR);
  }

  @Override
  public RCImageDescriptionType<RCImageDepthFormatType> declareDepthImage(
    final RCGNodeName name,
    final RCImageDepthFormatType format)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(format, "format");

    return this.declare(name, format, IMAGE_DEPTH);
  }

  @Override
  public void connect(
    final RCGPortSourceType<?> source,
    final RCGPortTargetType<?> target)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    if (!target.dataConstraint().isSatisfiedBy(source.dataConstraint())) {
      throw this.errorPortUnsatisfiedConstraint(source, target);
    }

    if (this.graph.inDegreeOf(target) != 0) {
      throw this.errorPortAlreadyConnected(source, target);
    }

    try {
      this.graph.addEdge(
        source,
        target,
        new RCGPortConnection(source, target)
      );
    } catch (final IllegalArgumentException e) {
      throw this.errorGraphCyclic(source, target);
    }
  }

  private RCGraphDescriptionException errorGraphCyclic(
    final RCGPortSourceType<?> source,
    final RCGPortTargetType<?> target)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_PORT_CYCLIC),
      Map.ofEntries(
        Map.entry(
          this.strings.format(GRAPH),
          this.graphName
        ),
        Map.entry(
          this.strings.format(SOURCE_NODE),
          source.owner().value()
        ),
        Map.entry(
          this.strings.format(SOURCE_PORT),
          source.name().value()
        ),
        Map.entry(
          this.strings.format(TARGET_NODE),
          target.owner().value()
        ),
        Map.entry(
          this.strings.format(TARGET_PORT),
          target.name().value()
        )
      ),
      PORT_CYCLIC_CONNECTION.codeName(),
      Optional.empty()
    );
  }

  private RCGraphDescriptionException errorPortAlreadyConnected(
    final RCGPortSourceType<?> source,
    final RCGPortTargetType<?> target)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_PORT_DUPLICATE_TARGET),
      Map.ofEntries(
        Map.entry(
          this.strings.format(GRAPH),
          this.graphName
        ),
        Map.entry(
          this.strings.format(SOURCE_NODE),
          source.owner().value()
        ),
        Map.entry(
          this.strings.format(SOURCE_PORT),
          source.name().value()
        ),
        Map.entry(
          this.strings.format(TARGET_NODE),
          target.owner().value()
        ),
        Map.entry(
          this.strings.format(TARGET_PORT),
          target.name().value()
        )
      ),
      DUPLICATE_PORT_CONNECTION.codeName(),
      Optional.empty()
    );
  }

  private RCGraphDescriptionException errorPortUnsatisfiedConstraint(
    final RCGPortSourceType<?> source,
    final RCGPortTargetType<?> target)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_PORT_CONSTRAINT_ERROR),
      Map.ofEntries(
        Map.entry(
          this.strings.format(GRAPH),
          this.graphName
        ),
        Map.entry(
          this.strings.format(SOURCE_NODE),
          source.owner().value()
        ),
        Map.entry(
          this.strings.format(SOURCE_PORT),
          source.name().value()
        ),
        Map.entry(
          this.strings.format(TARGET_NODE),
          target.owner().value()
        ),
        Map.entry(
          this.strings.format(TARGET_PORT),
          target.name().value()
        ),
        Map.entry(
          this.strings.format(SOURCE_PORT_PROVIDES),
          source.dataConstraint().explain()
        ),
        Map.entry(
          this.strings.format(TARGET_PORT_REQUIRES),
          target.dataConstraint().explain()
        )
      ),
      PORTS_INCOMPATIBLE.codeName(),
      Optional.empty()
    );
  }

  @Override
  public void validate()
    throws RCGraphDescriptionException
  {
    final var tracker =
      new ExceptionTracker<RCGraphDescriptionException>();

    for (final var port : this.graph.vertexSet()) {
      switch (port) {
        case final RCGPortSourceType<?> _ -> {

        }
        case final RCGPortTargetType<?> target -> {
          if (this.graph.inDegreeOf(target) != 1) {
            tracker.addException(this.errorPortNotConnected(target));
          }
        }
      }
    }

    tracker.throwIfNecessary();
  }

  private RCGraphDescriptionException errorPortNotConnected(
    final RCGPortTargetType<?> target)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_PORT_NOT_CONNECTED),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName),
        Map.entry(this.strings.format(TARGET_NODE), target.owner().value()),
        Map.entry(this.strings.format(TARGET_PORT), target.name().value())
      ),
      PORT_NOT_CONNECTED.codeName(),
      Optional.empty()
    );
  }

  private RCGraphDescriptionException errorGraphNodeExists(
    final RCGNodeName name,
    final String type)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_GRAPH_NODE_NAME_ALREADY_USED),
      Map.ofEntries(
        Map.entry(this.strings.format(TYPE), type),
        Map.entry(this.strings.format(GRAPH), this.graphName),
        Map.entry(this.strings.format(NODE), name.value())
      ),
      DUPLICATE_NODE.codeName(),
      Optional.empty()
    );
  }

  /**
   * @return The graph name
   */

  public String name()
  {
    return this.graphName;
  }

  /**
   * Build an immutable copy of this graph.
   *
   * @return The graph
   *
   * @throws RCGraphDescriptionException On errors
   */

  public RCGraphDescription build()
    throws RCGraphDescriptionException
  {
    this.validate();

    var requiredFeatures =
      VulkanPhysicalDeviceFeatures.builder()
        .setFeatures10(VulkanPhysicalDeviceFeatures10.builder().build())
        .setFeatures11(VulkanPhysicalDeviceFeatures11.builder().build())
        .setFeatures12(VulkanPhysicalDeviceFeatures12.builder().build())
        .setFeatures13(VulkanPhysicalDeviceFeatures13.builder().build())
        .build();

    for (var node : this.graphNodes.values()) {
      requiredFeatures =
        VulkanPhysicalDeviceFeaturesFunctions.or(
          requiredFeatures,
          node.requiredDeviceFeatures()
        );
    }

    return new RCGraphDescription(
      (DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection>) this.graph.clone(),
      Map.copyOf(this.graphNodes),
      requiredFeatures
    );
  }
}
