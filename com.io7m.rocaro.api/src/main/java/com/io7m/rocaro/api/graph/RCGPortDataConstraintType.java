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
 * A constraint placed upon data that a port can produce or consume.
 *
 * @param <T> The base type of data
 */

public interface RCGPortDataConstraintType<T>
{
  /**
   * @return The base class of data
   */

  Class<T> constrainedValueType();

  /**
   * Determine if data constrained by this constrained would satisfy the
   * other given constraint.
   *
   * @param other The other constraint
   *
   * @return {@code true} if the constraint is satisfied
   */

  boolean isSatisfiedBy(
    RCGPortDataConstraintType<?> other);

  /**
   * @param value The input value
   *
   * @return {@code true} if this constraint is satisfied by the given value
   */

  boolean isValid(T value);

  /**
   * @return An explanation of this constraint
   */

  String explain();
}
