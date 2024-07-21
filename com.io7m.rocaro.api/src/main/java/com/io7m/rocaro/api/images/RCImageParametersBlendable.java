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
 * The parameters for a blendable color image.
 *
 * @param size     The image size expression
 * @param channels The image channels
 */

public record RCImageParametersBlendable(
  RCImageSizeExpressionType size,
  RCImageColorChannels channels)
  implements RCImageParametersType
{
  private static final RCImageParametersBlendable WINDOW_SIZED_RGBA =
    new RCImageParametersBlendable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.RGBA
    );

  private static final RCImageParametersBlendable WINDOW_SIZED_R =
    new RCImageParametersBlendable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.R
    );

  private static final RCImageParametersBlendable WINDOW_SIZED_RG =
    new RCImageParametersBlendable(
      RCImageSizeExpressions.windowSized(),
      RCImageColorChannels.RG
    );

  /**
   * The parameters for a blendable color image.
   *
   * @param size     The image size expression
   * @param channels The image channels
   */

  public RCImageParametersBlendable
  {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(channels, "channels");
  }

  /**
   * @return Parameters for a window-sized RGBA image
   */

  public static RCImageParametersBlendable blendableWindowSizedRGBA()
  {
    return WINDOW_SIZED_RGBA;
  }

  /**
   * @return Parameters for a window-sized R image
   */

  public static RCImageParametersBlendable blendableWindowSizedR()
  {
    return WINDOW_SIZED_R;
  }

  /**
   * @return Parameters for a window-sized RG image
   */

  public static RCImageParametersBlendable blendableWindowSizedRG()
  {
    return WINDOW_SIZED_RG;
  }
}
