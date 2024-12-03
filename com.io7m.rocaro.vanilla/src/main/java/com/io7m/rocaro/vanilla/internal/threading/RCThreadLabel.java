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


package com.io7m.rocaro.vanilla.internal.threading;

/**
 * <p>
 * Labels applied to a thread. A thread label indicates which operations
 * a thread is allowed to perform. Broadly, this allows for dividing the
 * world into the graphics, compute, and transfer operations exposed by
 * Vulkan (that may all occur on different queues, and therefore different
 * threads).
 * </p>
 * <p>
 * Labels are treated as a 32-bit bitmask for efficient checking.
 * </p>
 */

public enum RCThreadLabel
{
  /**
   * The thread responsible for submitting graphics work.
   */

  GPU_GRAPHICS(0b00000000_00000000_00000000_00000001),

  /**
   * The thread responsible for submitting compute work.
   */

  GPU_COMPUTE(0b00000000_00000000_00000000_00000010),

  /**
   * The thread responsible for submitting transfer work.
   */

  GPU_TRANSFER(0b00000000_00000000_00000000_00000100),

  /**
   * The thread responsible for performing I/O to prepare for transfer work.
   */

  TRANSFER_IO(0b00000000_00000000_00000000_00001000),

  /**
   * The main coordinator thread for the renderer.
   */

  MAIN(0b00000000_00000000_00000000_00010000);

  private final int bit;

  RCThreadLabel(
    final int inBit)
  {
    this.bit = inBit;
  }

  /**
   * @return The label bit
   */

  public int bit()
  {
    return this.bit;
  }
}
