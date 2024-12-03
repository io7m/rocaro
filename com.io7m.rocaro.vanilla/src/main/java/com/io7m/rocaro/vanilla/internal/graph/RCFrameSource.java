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

import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGFrameNodeSourceType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodePreparationContextType;
import com.io7m.rocaro.api.graph.RCGNodeRenderContextType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducer;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;

import java.util.Map;
import java.util.Objects;

/**
 * The frame source.
 */

public final class RCFrameSource
  extends RCObject
  implements RCGFrameNodeSourceType
{
  private final RCGNodeName name;
  private final Map<RCGPortName, RCGPortType<?>> ports;
  private final RCGPortProducer<RCImageColorBlendableType> imageSource;

  /**
   * The frame source.
   *
   * @param inName        The node name
   * @param inPorts       The ports
   * @param inImageSource The image source
   */

  public RCFrameSource(
    final RCGNodeName inName,
    final Map<RCGPortName, RCGPortType<?>> inPorts,
    final RCGPortProducer<RCImageColorBlendableType> inImageSource)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.ports =
      Map.copyOf(inPorts);
    this.imageSource =
      Objects.requireNonNull(inImageSource, "imageSource");
  }

  @Override
  public RCGNodeName name()
  {
    return this.name;
  }

  @Override
  public RCUnit parameters()
  {
    return RCUnit.UNIT;
  }

  @Override
  public Map<RCGPortName, RCGPortType<?>> ports()
  {
    return this.ports;
  }

  @Override
  public void prepare(
    final RCGNodePreparationContextType context)
  {

  }

  @Override
  public void evaluate(
    final RCGNodeRenderContextType context)
  {
    final var vulkan =
      context.frameScopedService(RCVulkanFrameContextType.class);
    final var windowContext =
      vulkan.windowFrameContext();

    context.portWrite(this.imageSource, windowContext.image());
  }
}
