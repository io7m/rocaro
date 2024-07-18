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
import com.io7m.rocaro.api.render_pass.RCRenderPassDescriptionType;

import java.util.Map;

/**
 * A description of a node in the render graph.
 *
 * @param <P> The type of parameters
 */

public sealed interface RCGNodeDescriptionType<P>
  permits RCRenderPassDescriptionType,
  RCGResourceDescriptionType
{
  /**
   * @return The node parameters
   */

  P parameters();

  /**
   * @return The node name
   */

  RCGNodeName name();

  /**
   * @return The node ports
   */

  Map<RCGPortName, RCGPortType<?>> ports();

  /**
   * @return The physical device features required for this node
   */

  VulkanPhysicalDeviceFeatures requiredDeviceFeatures();
}
