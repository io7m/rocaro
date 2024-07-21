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

/**
 * A frame index is the current frame number modulo the maximum number of
 * frames in-flight.
 *
 * @param value The value
 *
 * @see RCFrameNumber
 */

public record RCFrameIndex(
  int value)
  implements Comparable<RCFrameIndex>
{
  /**
   * A frame index is the current frame number modulo the maximum number of
   * frames in-flight.
   *
   * @param value The value
   *
   * @see RCFrameNumber
   */

  public RCFrameIndex
  {
    if (value < 0 || value > 1000) {
      throw new IllegalArgumentException(
        "Frame indices must be in the range [0, 1000]");
    }
  }

  @Override
  public String toString()
  {
    return "[RCFrameIndex %s]".formatted(Integer.toUnsignedString(this.value));
  }

  @Override
  public int compareTo(
    final RCFrameIndex other)
  {
    return Integer.compareUnsigned(this.value, other.value);
  }
}
