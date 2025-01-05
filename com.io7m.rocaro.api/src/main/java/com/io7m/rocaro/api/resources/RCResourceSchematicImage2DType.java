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


package com.io7m.rocaro.api.resources;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

/**
 * A schematic for a 2D image resource; the information that fully describes a
 * run-time resource value.
 *
 * @see com.io7m.rocaro.api.images.RCImage2DType
 */

public non-sealed interface RCResourceSchematicImage2DType
  extends RCResourceSchematicImageType
{
  /**
   * @return The size of the image
   */

  Vector2I size();

  /**
   * @return {@code true} if the image is a presentable window surface
   */

  boolean isPresentationImage();
}
