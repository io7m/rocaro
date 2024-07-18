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

import com.io7m.rocaro.api.images.RCImageColorFormat;
import com.io7m.rocaro.api.images.RCImageDepthFormatType;
import com.io7m.rocaro.api.images.RCImageDescriptionType;

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
   * @param <D>         The type of node descriptions
   * @param <N>         The type of nodes
   *
   * @return A node description
   *
   * @throws RCGraphDescriptionException On errors
   */

  <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P>> D declare(
    RCGNodeName name,
    P parameters,
    RCGNodeFactoryType<P, N, D> nodeFactory)
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

  default <P, N extends RCGNodeType<P>, D extends RCGNodeDescriptionType<P>> D declare(
    final String name,
    final P parameters,
    final RCGNodeFactoryType<P, N, D> nodeFactory)
    throws RCGraphDescriptionException
  {
    return this.declare(new RCGNodeName(name), parameters, nodeFactory);
  }

  /**
   * Declare a color image node with the given format.
   *
   * @param name   The node name
   * @param format The format
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageDescriptionType<RCImageColorFormat> declareColorImage(
    RCGNodeName name,
    RCImageColorFormat format)
    throws RCGraphDescriptionException;

  /**
   * Declare a color image node with the given format.
   *
   * @param name   The node name
   * @param format The format
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageDescriptionType<RCImageColorFormat> declareColorImage(
    final String name,
    final RCImageColorFormat format)
    throws RCGraphDescriptionException
  {
    return this.declareColorImage(new RCGNodeName(name), format);
  }

  /**
   * Declare a depth image node with the given format.
   *
   * @param name   The node name
   * @param format The format
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCImageDescriptionType<RCImageDepthFormatType> declareDepthImage(
    RCGNodeName name,
    RCImageDepthFormatType format)
    throws RCGraphDescriptionException;

  /**
   * Declare a depth image node with the given format.
   *
   * @param name   The node name
   * @param format The format
   *
   * @return An image description
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCImageDescriptionType<RCImageDepthFormatType> declareDepthImage(
    final String name,
    final RCImageDepthFormatType format)
    throws RCGraphDescriptionException
  {
    return this.declareDepthImage(new RCGNodeName(name), format);
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
