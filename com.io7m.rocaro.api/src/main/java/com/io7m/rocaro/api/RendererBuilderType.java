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


package com.io7m.rocaro.api;

import com.io7m.rocaro.api.displays.RCDisplaySelectionType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionBuilderType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.graph.RCGraphName;

/**
 * A mutable builder used to construct a renderer.
 */

public interface RendererBuilderType
{
  /**
   * Declare a new render graph. Graph names must be unique within a renderer.
   *
   * @param name The name of the graph
   *
   * @return A graph builder
   *
   * @throws RCGraphDescriptionException On errors
   */

  RCGraphDescriptionBuilderType declareRenderGraph(
    RCGraphName name)
    throws RCGraphDescriptionException;

  /**
   * Declare a new render graph. Graph names must be unique within a renderer.
   *
   * @param name The name of the graph
   *
   * @return A graph builder
   *
   * @throws RCGraphDescriptionException On errors
   */

  default RCGraphDescriptionBuilderType declareRenderGraph(
    final String name)
    throws RCGraphDescriptionException
  {
    return this.declareRenderGraph(new RCGraphName(name));
  }

  /**
   * Set the method used to select a display.
   *
   * @param selection The selection method
   *
   * @return this
   */

  RendererBuilderType setDisplaySelection(
    RCDisplaySelectionType selection);

  /**
   * Set the Vulkan configuration for the renderer.
   *
   * @param vulkanConfiguration The configuration
   *
   * @return this
   */

  RendererBuilderType setVulkanConfiguration(
    RendererVulkanConfiguration vulkanConfiguration);

  /**
   * Create and start a new renderer based on all of the information given
   * so far.
   *
   * @return A renderer
   *
   * @throws RocaroException On errors
   */

  RendererType start()
    throws RocaroException;
}
