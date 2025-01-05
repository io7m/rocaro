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


package com.io7m.rocaro.vanilla.internal.vulkan;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.graph.RCGResourceSubname;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetSchematicType;
import com.io7m.rocaro.api.resources.RCDepthComponents;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.api.resources.RCResourceSchematicPrimitiveType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A schematic for frame render targets.
 *
 *
 * @param colorAttachment The color attachment
 */

public record RCFrameRenderTargetSchematic(
  RCFrameImageSchematic colorAttachment)
  implements RCPresentationRenderTargetSchematicType
{
  /**
   * A schematic for frame render targets.
   *
   * @param colorAttachment The color attachment
   */

  public RCFrameRenderTargetSchematic
  {
    Objects.requireNonNull(colorAttachment, "colorAttachment");
  }

  @Override
  public Vector2I size()
  {
    return this.colorAttachment.size();
  }

  @Override
  public List<RCResourceSchematicImage2DType> colorAttachments()
  {
    return List.of(this.colorAttachment);
  }

  @Override
  public Optional<RCDepthComponents> depthAttachment()
  {
    return Optional.empty();
  }

  @Override
  public Map<RCGResourceSubname, RCResourceSchematicPrimitiveType> schematics()
  {
    return Map.of(
      new RCGResourceSubname("Color"),
      this.colorAttachment
    );
  }
}
