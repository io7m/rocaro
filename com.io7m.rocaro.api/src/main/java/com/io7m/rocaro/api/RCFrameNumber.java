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


package com.io7m.rocaro.api;

import java.math.BigInteger;
import java.util.Objects;

/**
 * The number of the current frame.
 *
 * @param value The frame number
 */

public record RCFrameNumber(
  BigInteger value)
  implements Comparable<RCFrameNumber>
{
  /**
   * The number of the current frame.
   *
   * @param value The frame number
   */

  public RCFrameNumber
  {
    Objects.requireNonNull(value, "value");

    if (value.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException(
        "Frame numbers must be non-negative.");
    }
  }

  /**
   * @return The first frame number
   */

  public static RCFrameNumber first()
  {
    return new RCFrameNumber(BigInteger.ZERO);
  }

  /**
   * @return The next frame number
   */

  public RCFrameNumber next()
  {
    return new RCFrameNumber(this.value.add(BigInteger.ONE));
  }

  @Override
  public int compareTo(
    final RCFrameNumber other)
  {
    return this.value.compareTo(other.value);
  }

  @Override
  public String toString()
  {
    return "[RCFrameNumber %s]".formatted(this.value);
  }

  /**
   * Convert this number to a frame index.
   *
   * @param maxFrames The maximum number of frames
   *
   * @return The index
   */

  public RCFrameIndex toFrameIndex(
    final int maxFrames)
  {
    final var imageCount =
      BigInteger.valueOf((long) maxFrames);
    final var frameMod =
      this.value.mod(imageCount);

    return new RCFrameIndex(frameMod.intValueExact());
  }
}
