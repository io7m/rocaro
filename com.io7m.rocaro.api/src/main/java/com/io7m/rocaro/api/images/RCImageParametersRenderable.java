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

import java.util.Objects;

/**
 * The parameters for a renderable color image.
 *
 * @param size     The image size expression
 * @param channels The image channels
 */

public record RCImageParametersRenderable(
  RCImageSizeExpressionType size,
  RCImageColorChannels channels)
  implements RCImageParametersType
{
  private static final RCImageParametersRenderable WINDOW_SIZED_RGBA =
    new RCImageParametersRenderable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.RGBA
    );

  private static final RCImageParametersRenderable WINDOW_SIZED_R =
    new RCImageParametersRenderable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.R
    );

  private static final RCImageParametersRenderable WINDOW_SIZED_RG =
    new RCImageParametersRenderable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.RG
    );

  /**
   * The parameters for a renderable color image.
   *
   * @param size     The image size expression
   * @param channels The image channels
   */

  public RCImageParametersRenderable
  {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(channels, "channels");
  }

  /**
   * @return Parameters for a window-sized RGBA image
   */

  public static RCImageParametersRenderable renderableWindowSizedRGBA()
  {
    return WINDOW_SIZED_RGBA;
  }

  /**
   * @return Parameters for a window-sized R image
   */

  public static RCImageParametersRenderable renderableWindowSizedR()
  {
    return WINDOW_SIZED_R;
  }

  /**
   * @return Parameters for a window-sized RG image
   */

  public static RCImageParametersRenderable renderableWindowSizedRG()
  {
    return WINDOW_SIZED_RG;
  }
}
