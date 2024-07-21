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

import com.io7m.rocaro.api.RCUnit;
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

/**
 * A render graph description builder.
 */

public interface RCGraphDescriptionBuilderType
{
  /**
   * Declare a node.
   *
   * @param name        The node name
   * @param parameters  The parameters
   * @param nodeFactory The node factory
   * @param <P>         The type of parameters
   * @param <N>         The type of nodes
   * @param <D>         The type of node descriptions
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P, N>> D declare(
    RCGNodeName name,
    P parameters,
    RCGNodeDescriptionFactoryType<P, N, D> nodeFactory)
    throws RCGraphDescriptionException;

  /**
   * Declare a node.
   *
   * @param name        The node name
   * @param parameters  The parameters
   * @param nodeFactory The node factory
   * @param <P>         The type of parameters
   * @param <D>         The type of node descriptions
   * @param <N>         The type of nodes
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P, N>> D declare(
    final String name,
    final P parameters,
    final RCGNodeDescriptionFactoryType<P, N, D> nodeFactory)
    throws RCGraphDescriptionException
  {
    return this.declare(new RCGNodeName(name), parameters, nodeFactory);
  }

  /**
   * Declare the frame source. There must be exactly one frame source in
   * every render graph.
   *
   * @param name The node name
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCGFrameNodeSourceDescriptionType declareFrameSource(
    RCGNodeName name)
    throws RCGraphDescriptionException;

  /**
   * Declare the frame source. There must be exactly one frame source in
   * every render graph.
   *
   * @param name The node name
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCGFrameNodeSourceDescriptionType declareFrameSource(
    final String name)
    throws RCGraphDescriptionException
  {
    return this.declareFrameSource(new RCGNodeName(name));
  }

  /**
   * Declare the frame target. There must be exactly one frame target in
   * every render graph.
   *
   * @param name The node name
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCGFrameNodeTargetDescriptionType declareFrameTarget(
    RCGNodeName name)
    throws RCGraphDescriptionException;

  /**
   * Declare the frame target. There must be exactly one frame target in
   * every render graph.
   *
   * @param name The node name
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCGFrameNodeTargetDescriptionType declareFrameTarget(
    final String name)
    throws RCGraphDescriptionException
  {
    return this.declareFrameTarget(new RCGNodeName(name));
  }

  /**
   * Declare a color renderable image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType>
  declareColorRenderableImage(
    RCGNodeName name,
    RCImageParametersRenderable parameters)
    throws RCGraphDescriptionException;

  /**
   * Declare a color renderable image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType>
  declareColorRenderableImage(
    final String name,
    final RCImageParametersRenderable parameters)
    throws RCGraphDescriptionException
  {
    return this.declareColorRenderableImage(new RCGNodeName(name), parameters);
  }

  /**
   * Declare a color blendable image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageNodeDescriptionType<RCImageParametersBlendable, RCImageColorBlendableType>
  declareColorBlendableImage(
    RCGNodeName name,
    RCImageParametersBlendable parameters)
    throws RCGraphDescriptionException;

  /**
   * Declare a color blendable image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageNodeDescriptionType<RCImageParametersBlendable, RCImageColorBlendableType>
  declareColorBlendableImage(
    final String name,
    final RCImageParametersBlendable parameters)
    throws RCGraphDescriptionException
  {
    return this.declareColorBlendableImage(new RCGNodeName(name), parameters);
  }

  /**
   * Declare a depth image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageNodeDescriptionType<RCImageParametersDepth, RCImageDepthType>
  declareDepthImage(
    RCGNodeName name,
    RCImageParametersDepth parameters)
    throws RCGraphDescriptionException;

  /**
   * Declare a depth image node with the given format.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageNodeDescriptionType<RCImageParametersDepth, RCImageDepthType>
  declareDepthImage(
    final String name,
    final RCImageParametersDepth parameters)
    throws RCGraphDescriptionException
  {
    return this.declareDepthImage(new RCGNodeName(name), parameters);
  }

  /**
   * Declare a depth+stencil image node.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageNodeDescriptionType<RCImageParametersDepthStencil, RCImageDepthStencilType>
  declareDepthStencilImage(
    RCGNodeName name,
    RCImageParametersDepthStencil parameters)
    throws RCGraphDescriptionException;

  /**
   * Declare a depth+stencil image node with the given format.
   *
   * @param name       The node name
   * @param parameters The parameters
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageNodeDescriptionType<RCImageParametersDepthStencil, RCImageDepthStencilType>
  declareDepthStencilImage(
    final String name,
    final RCImageParametersDepthStencil parameters)
    throws RCGraphDescriptionException
  {
    return this.declareDepthStencilImage(new RCGNodeName(name), parameters);
  }

  /**
   * Declare a render pass that does nothing.
   *
   * @param name The node name
   *
   * @return A render pass description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCRenderPassDescriptionType<RCUnit, RCRenderPassType<RCUnit>> declareEmptyRenderPass(
    RCGNodeName name)
    throws RCGraphDescriptionException;

  /**
   * Declare a render pass that does nothing.
   *
   * @param name The node name
   *
   * @return A render pass description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCRenderPassDescriptionType<RCUnit, RCRenderPassType<RCUnit>> declareEmptyRenderPass(
    final String name)
    throws RCGraphDescriptionException
  {
    return this.declareEmptyRenderPass(new RCGNodeName(name));
  }

  /**
   * Connect the given ports.
   *
   * @param source The source port
   * @param target The target port
   *
   * @throws RCGraphDescriptionException On errors
   */

  void connect(
    RCGPortSourceType<?> source,
    RCGPortTargetType<?> target)
    throws RCGraphDescriptionException;

  /**
   * Validate the current graph description.
   *
   * @throws RCGraphDescriptionException On errors
   */

  void validate()
    throws RCGraphDescriptionException;
}
