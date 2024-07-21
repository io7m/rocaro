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

/**
 * An image size expression that specifies an image that is a fraction of
 * the current window size. A fraction of {@code 1.0} represents an image
 * exactly the same size as the window. A fraction of {@code 2.0} represents an
 * image exactly twice the size of the window. A fraction of {@code 0.5}
 * represents an image exactly half the size of the window.
 *
 * @param fraction The image size fraction
 */

public record RCImageSizeWindowFraction(
  double fraction)
  implements RCImageSizeExpressionType
{
  /**
   * An image size expression that specifies an image that is a fraction of
   * the current window size. A fraction of {@code 1.0} represents an image
   * exactly the same size as the window. A fraction of {@code 2.0} represents an
   * image exactly twice the size of the window. A fraction of {@code 0.5}
   * represents an image exactly half the size of the window.
   *
   * @param fraction The image size fraction
   */

  public RCImageSizeWindowFraction
  {
    fraction = Math.clamp(fraction, 0.0, 32.0);
  }

  @Override
  public String explain()
  {
    return "(size ≡ window.size() × %f)".formatted(this.fraction);
  }
}
