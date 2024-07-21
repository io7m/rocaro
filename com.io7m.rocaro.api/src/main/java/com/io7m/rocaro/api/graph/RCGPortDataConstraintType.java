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


package com.io7m.rocaro.api.graph;

/**
 * A constraint on the data produced and/or consumed by ports.
 *
 * @param <T> The type of port data
 */

public interface RCGPortDataConstraintType<T>
{
  /**
   * @return The port data type
   */

  Class<T> dataType();

  /**
   * Determine if the constraint on this port is satisfied by the constraint
   * on the other given port. This is typically used such that {@code this}
   * is the <i>target</i> port and <i>other</i> is the constraint on the
   * <i>source</i> port; we are asking whether {@code this} port can consume
   * data produced by the other port.
   *
   * @param other The constraint on the other port
   *
   * @return {@code true} if the constraint is satisfied
   */

  boolean isSatisfiedBy(
    RCGPortDataConstraintType<?> other);

  /**
   * @return A humanly-readable explanation of this constraint
   */

  String explain();
}
