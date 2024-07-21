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
import java.util.Optional;

/**
 * Functions over size expressions.
 */

public final class RCImageSizeExpressions
{
  private static final RCImageSizeWindowFraction WINDOW_SIZED =
    new RCImageSizeWindowFraction(1.0);

  private static final RCImageSizeWindowFraction WINDOW_SIZED_HALF =
    new RCImageSizeWindowFraction(0.5);

  private RCImageSizeExpressions()
  {

  }

  /**
   * Determine if the given {@code provides} expression satisfies the given
   * {@code requires} expression.
   *
   * @param requires The required expression
   * @param provides The provided expression
   *
   * @return {@code true} if {@code provides} satisfies {@code requires}
   */

  public static boolean satisfies(
    final Optional<RCImageSizeExpressionType> requires,
    final Optional<RCImageSizeExpressionType> provides)
  {
    Objects.requireNonNull(requires, "requires");
    Objects.requireNonNull(provides, "provides");

    if (requires.isEmpty()) {
      return true;
    }
    return requires.equals(provides);
  }

  /**
   * @return A size expression that matches the size of the window
   */

  public static RCImageSizeWindowFraction windowSized()
  {
    return WINDOW_SIZED;
  }

  /**
   * @return A size expression that matches exactly half the size of the window
   */

  public static RCImageSizeWindowFraction windowSizedHalf()
  {
    return WINDOW_SIZED_HALF;
  }
}
