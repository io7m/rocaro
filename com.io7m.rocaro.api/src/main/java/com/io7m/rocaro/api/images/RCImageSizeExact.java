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


package com.io7m.rocaro.api.images;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2I;

import java.util.Objects;

/**
 * An image size expression that specifies an exact image size.
 *
 * @param size The image size in pixels/texels
 */

public record RCImageSizeExact(
  Vector2I size)
  implements RCImageSizeExpressionType
{
  /**
   * An image size expression that specifies an exact image size.
   *
   * @param size The image size in  pixels/texels
   */

  public RCImageSizeExact
  {
    Objects.requireNonNull(size, "size");

    size = Vectors2I.clamp(
      size,
      Vector2I.of(1, 1),
      Vector2I.of(Integer.MAX_VALUE, Integer.MAX_VALUE)
    );
  }

  @Override
  public String explain()
  {
    return String.format("(size ≡ %dx%d)", this.size.x(), this.size.y());
  }
}
