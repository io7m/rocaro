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
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGFrameNodeDescriptionType;
import com.io7m.rocaro.api.graph.RCGFrameNodeSourceDescriptionType;
import com.io7m.rocaro.api.graph.RCGFrameNodeTargetDescriptionType;
import com.io7m.rocaro.api.graph.RCGNodeDescriptionFactoryType;
import com.io7m.rocaro.api.graph.RCGNodeDescriptionType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodeType;
import com.io7m.rocaro.api.graph.RCGPortConnection;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortTargetType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceDescriptionType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionBuilderType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.api.images.RCImageColorRenderableType;
import com.io7m.rocaro.api.images.RCImageDepthStencilType;
import com.io7m.rocaro.api.images.RCImageDepthType;
import com.io7m.rocaro.api.images.RCImageNodeDescriptionType;
import com.io7m.rocaro.api.images.RCImageParametersBlendable;
import com.io7m.rocaro.api.images.RCImageParametersDepth;
import com.io7m.rocaro.api.images.RCImageParametersDepthStencil;
import com.io7m.rocaro.api.images.RCImageParametersRenderable;
import com.io7m.rocaro.api.render_pass.RCRenderPassDescriptionType;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_FRAME_SOURCE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_FRAME_TARGET;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_NODE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_PORT_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.NONEXISTENT_FRAME_SOURCE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.NONEXISTENT_FRAME_TARGET;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORTS_INCOMPATIBLE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_CYCLIC_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_NOT_CONNECTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_SOURCE_EXISTS;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_SOURCE_NONEXISTENT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_SOURCE_NONEXISTENT_REMEDIATE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_TARGET_EXISTS;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_TARGET_NONEXISTENT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_FRAME_TARGET_NONEXISTENT_REMEDIATE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NODE_NAME_ALREADY_USED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_CONSTRAINT_ERROR;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_CYCLIC;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_DUPLICATE_TARGET;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_NOT_CONNECTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.NODE_EXISTING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_PORT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.SOURCE_PORT_PROVIDES;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_NODE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_PORT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TARGET_PORT_REQUIRES;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.TYPE;
import static com.io7m.rocaro.vanilla.internal.graph.RCFrameSourceDescriptionFactory.FRAME_SOURCES;
import static com.io7m.rocaro.vanilla.internal.graph.RCFrameTargetDescriptionFactory.FRAME_TARGETS;
import static com.io7m.rocaro.vanilla.internal.images.RCImageNodeColorBlendable.IMAGE_NODE_COLOR_BLENDABLE;
import static com.io7m.rocaro.vanilla.internal.images.RCImageNodeColorRenderable.IMAGE_NODE_COLOR_RENDERABLE;
import static com.io7m.rocaro.vanilla.internal.images.RCImageNodeDepth.IMAGE_NODE_DEPTH;
import static com.io7m.rocaro.vanilla.internal.images.RCImageNodeDepthStencil.IMAGE_NODE_DEPTH_STENCIL;
import static com.io7m.rocaro.vanilla.internal.renderpass.empty.RCRenderPassEmpty.RENDER_PASS_EMPTY;

/**
 * A render graph builder.
 */

public final class RCGraphDescriptionBuilder
  implements RCGraphDescriptionBuilderType
{
  private final RCStrings strings;
  private final RCGraphName graphName;
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph;
  private final HashMap<RCGNodeName, RCGNodeDescriptionType<?, ?>> graphNodes;
  private Optional<RCGFrameNodeSourceDescriptionType> frameSource;
  private Optional<RCGFrameNodeTargetDescriptionType> frameTarget;

  /**
   * A render graph builder.
   *
   * @param inStrings The string resources
   * @param inName    The graph name
   */

  public RCGraphDescriptionBuilder(
    final RCStrings inStrings,
    final RCGraphName inName)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.graphName =
      Objects.requireNonNull(inName, "name");
    this.graphNodes =
      new HashMap<>();
    this.graph =
      new DirectedAcyclicGraph<>(RCGPortConnection.class);

    this.frameSource = Optional.empty();
    this.frameTarget = Optional.empty();
  }

  @Override
  public <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P, N>> D declare(
    final RCGNodeName name,
    final P parameters,
    final RCGNodeDescriptionFactoryType<P, N, D> nodeFactory)
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

    switch (node) {
      case final RCGFrameNodeDescriptionType<?> frameNode -> {
        switch (frameNode) {
          case final RCGFrameNodeSourceDescriptionType source -> {
            if (this.frameSource.isPresent()) {
              throw this.errorFrameSourceExists(this.frameSource.get(), name);
            }
            this.frameSource = Optional.of(source);
          }
          case final RCGFrameNodeTargetDescriptionType target -> {
            if (this.frameTarget.isPresent()) {
              throw this.errorFrameTargetExists(this.frameTarget.get(), name);
            }
            this.frameTarget = Optional.of(target);
          }
        }
      }
      case final RCGResourceDescriptionType<?, ?> _,
           final RCRenderPassDescriptionType<?, ?> _ -> {
        // Nothing
      }
    }

    if (node instanceof final RCGFrameNodeSourceDescriptionType fs) {
      this.frameSource = Optional.of(fs);
    }

    this.graphNodes.put(name, node);
    for (final var port : node.ports().values()) {
      this.graph.addVertex(port);
    }

    return node;
  }

  @Override
  public RCGFrameNodeSourceDescriptionType declareFrameSource(
    final RCGNodeName name)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    return this.declare(name, RCUnit.UNIT, FRAME_SOURCES);
  }

  @Override
  public RCGFrameNodeTargetDescriptionType declareFrameTarget(
    final RCGNodeName name)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    return this.declare(name, RCUnit.UNIT, FRAME_TARGETS);
  }

  @Override
  public RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType>
  declareColorRenderableImage(
    final RCGNodeName name,
    final RCImageParametersRenderable parameters)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    return this.declare(name, parameters, IMAGE_NODE_COLOR_RENDERABLE);
  }

  @Override
  public RCImageNodeDescriptionType<RCImageParametersBlendable, RCImageColorBlendableType>
  declareColorBlendableImage(
    final RCGNodeName name,
    final RCImageParametersBlendable parameters)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    return this.declare(name, parameters, IMAGE_NODE_COLOR_BLENDABLE);
  }

  @Override
  public RCImageNodeDescriptionType<RCImageParametersDepth, RCImageDepthType>
  declareDepthImage(
    final RCGNodeName name,
    final RCImageParametersDepth parameters)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    return this.declare(name, parameters, IMAGE_NODE_DEPTH);
  }

  @Override
  public RCImageNodeDescriptionType<RCImageParametersDepthStencil, RCImageDepthStencilType>
  declareDepthStencilImage(
    final RCGNodeName name,
    final RCImageParametersDepthStencil parameters)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(parameters, "parameters");
    return this.declare(name, parameters, IMAGE_NODE_DEPTH_STENCIL);
  }

  @Override
  public RCRenderPassDescriptionType<RCUnit, RCRenderPassType<RCUnit>>
  declareEmptyRenderPass(
    final RCGNodeName name)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");
    return this.declare(name, RCUnit.UNIT, RENDER_PASS_EMPTY);
  }

  @Override
  public void connect(
    final RCGPortSourceType<?> source,
    final RCGPortTargetType<?> target)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    final var targetConstraint =
      target.dataConstraint();
    final var sourceConstraint =
      source.dataConstraint();
    final var targetType =
      targetConstraint.dataType();
    final var sourceType =
      sourceConstraint.dataType();

    if (!sourceType.isAssignableFrom(targetType)) {
      throw this.errorPortUnsatisfiedConstraint(source, target);
    }

    if (!targetConstraint.isSatisfiedBy(sourceConstraint)) {
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
          this.graphName.value()
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
          this.graphName.value()
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
          this.graphName.value()
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

    if (this.frameSource.isEmpty()) {
      tracker.addException(this.errorFrameSourceNonexistent());
    }

    if (this.frameTarget.isEmpty()) {
      tracker.addException(this.errorFrameTargetNonexistent());
    }

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

  private RCGraphDescriptionException errorFrameTargetNonexistent()
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_FRAME_TARGET_NONEXISTENT),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName.value())
      ),
      NONEXISTENT_FRAME_TARGET.codeName(),
      Optional.of(
        this.strings.format(ERROR_FRAME_TARGET_NONEXISTENT_REMEDIATE)
      )
    );
  }

  private RCGraphDescriptionException errorFrameSourceNonexistent()
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_FRAME_SOURCE_NONEXISTENT),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName.value())
      ),
      NONEXISTENT_FRAME_SOURCE.codeName(),
      Optional.of(
        this.strings.format(ERROR_FRAME_SOURCE_NONEXISTENT_REMEDIATE)
      )
    );
  }

  private RCGraphDescriptionException errorPortNotConnected(
    final RCGPortTargetType<?> target)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_PORT_NOT_CONNECTED),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName.value()),
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
        Map.entry(this.strings.format(GRAPH), this.graphName.value()),
        Map.entry(this.strings.format(NODE), name.value())
      ),
      DUPLICATE_NODE.codeName(),
      Optional.empty()
    );
  }

  private RCGraphDescriptionException errorFrameSourceExists(
    final RCGFrameNodeSourceDescriptionType description,
    final RCGNodeName name)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_FRAME_SOURCE_EXISTS),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName.value()),
        Map.entry(this.strings.format(NODE), name.value()),
        Map.entry(
          this.strings.format(NODE_EXISTING),
          description.name().value())
      ),
      DUPLICATE_FRAME_SOURCE.codeName(),
      Optional.empty()
    );
  }

  private RCGraphDescriptionException errorFrameTargetExists(
    final RCGFrameNodeTargetDescriptionType description,
    final RCGNodeName name)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_FRAME_TARGET_EXISTS),
      Map.ofEntries(
        Map.entry(this.strings.format(GRAPH), this.graphName.value()),
        Map.entry(this.strings.format(NODE), name.value()),
        Map.entry(
          this.strings.format(NODE_EXISTING),
          description.name().value())
      ),
      DUPLICATE_FRAME_TARGET.codeName(),
      Optional.empty()
    );
  }


  /**
   * @return The graph name
   */

  public RCGraphName name()
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

    for (final var node : this.graphNodes.values()) {
      requiredFeatures =
        VulkanPhysicalDeviceFeaturesFunctions.or(
          requiredFeatures,
          node.requiredDeviceFeatures()
        );
    }

    return new RCGraphDescription(
      this.graphName,
      (DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection>) this.graph.clone(),
      Map.copyOf(this.graphNodes),
      requiredFeatures
    );
  }
}
