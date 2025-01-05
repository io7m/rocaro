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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;

import java.util.Objects;

/**
 * A 2D image schematic.
 *
 * @param size                The image size
 * @param format              The image format
 * @param isPresentationImage Whether the image is a presentation image
 */

public record RCImage2DSchematic(
  Vector2I size,
  VulkanFormat format,
  boolean isPresentationImage)
  implements RCResourceSchematicImage2DType
{
  /**
   * A 2D image schematic.
   *
   * @param size                The image size
   * @param format              The image format
   * @param isPresentationImage Whether the image is a presentation image
   */

  public RCImage2DSchematic
  {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(format, "format");
  }
}
