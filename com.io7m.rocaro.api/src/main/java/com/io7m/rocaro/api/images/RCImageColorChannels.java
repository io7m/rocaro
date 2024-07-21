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
 * A constraint on the set of channels in a color image.
 */

public enum RCImageColorChannels
{
  /**
   * The image has at least a red channel.
   */

  R {
    @Override
    public boolean isSatisfiedBy(
      final RCImageColorChannels other)
    {
      return true;
    }
  },

  /**
   * The image has at least red and green channels.
   */

  RG {
    @Override
    public boolean isSatisfiedBy(
      final RCImageColorChannels other)
    {
      return switch (other) {
        case R -> false;
        case RG -> true;
        case RGB -> true;
        case RGBA -> true;
      };
    }
  },

  /**
   * The image has at least red, green, and blue channels.
   */

  RGB {
    @Override
    public boolean isSatisfiedBy(
      final RCImageColorChannels other)
    {
      return switch (other) {
        case R -> false;
        case RG -> false;
        case RGB -> true;
        case RGBA -> true;
      };
    }
  },

  /**
   * The image has at least red, green, blue, and alpha channels.
   */

  RGBA {
    @Override
    public boolean isSatisfiedBy(
      final RCImageColorChannels other)
    {
      return switch (other) {
        case R -> false;
        case RG -> false;
        case RGB -> false;
        case RGBA -> true;
      };
    }
  };

  /**
   * Determine if these channels could be satisfied by a set of other channels.
   * This is a subset operation: If code requires an image with a red channel,
   * then it can be satisfied by any color image that has at least a red
   * channel (and may have more channels).
   *
   * @param other The other channels
   *
   * @return {@code true} if this constraint is satisfied by {@code other}
   */

  public abstract boolean isSatisfiedBy(
    RCImageColorChannels other);

  /**
   * @return The explanation of this channel set
   */

  public String explain()
  {
    return "(channels ⊂ %s)".formatted(this.name());
  }
}
