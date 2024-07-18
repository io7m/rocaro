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


package com.io7m.rocaro.api.displays;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3I;

import java.util.Comparator;
import java.util.Objects;

/**
 * <p>A display mode.</p>
 *
 * <p>Display modes can be compared; modes with larger sizes in pixels,
 * larger refresh rates, and more bits in the red/green/blue channels are
 * considered "greater" (in that order).</p>
 *
 * @param sizePixels    The size of the display in pixels at this mode
 * @param colorBits     The size in bits of the red/green/blue channels at this mode
 * @param refreshRateHZ The refresh rate in hertz at this mode
 */

public record RCDisplayMode(
  Vector2I sizePixels,
  Vector3I colorBits,
  double refreshRateHZ)
  implements Comparable<RCDisplayMode>
{
  /**
   * <p>A display mode.</p>
   *
   * <p>Display modes can be compared; modes with larger sizes in pixels,
   * larger refresh rates, and more bits in the red/green/blue channels are
   * considered "greater" (in that order).</p>
   *
   * @param sizePixels    The size of the display in pixels at this mode
   * @param colorBits     The size in bits of the red/green/blue channels at this mode
   * @param refreshRateHZ The refresh rate in hertz at this mode
   */

  public RCDisplayMode
  {
    Objects.requireNonNull(sizePixels, "sizePixels");
    Objects.requireNonNull(colorBits, "colorBits");
  }

  /**
   * @return The width of the display in pixels at this mode
   */

  public int widthPixels()
  {
    return this.sizePixels.x();
  }

  /**
   * @return The height of the display in pixels at this mode
   */

  public int heightPixels()
  {
    return this.sizePixels.y();
  }

  /**
   * @return The number of bits in the red channel at this mode
   */

  public int redBits()
  {
    return this.colorBits.x();
  }

  /**
   * @return The number of bits in the green channel at this mode
   */

  public int greenBits()
  {
    return this.colorBits.y();
  }

  /**
   * @return The number of bits in the blue channel at this mode
   */

  public int blueBits()
  {
    return this.colorBits.z();
  }

  @Override
  public int compareTo(
    final RCDisplayMode other)
  {
    return Comparator.comparingInt(RCDisplayMode::widthPixels)
      .thenComparingInt(RCDisplayMode::heightPixels)
      .thenComparingDouble(RCDisplayMode::refreshRateHZ)
      .thenComparingInt(RCDisplayMode::redBits)
      .thenComparingInt(RCDisplayMode::greenBits)
      .thenComparingInt(RCDisplayMode::blueBits)
      .compare(this, other);
  }
}
