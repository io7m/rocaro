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


package com.io7m.rocaro.tests.arbitraries;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.images.RCImageSizeExact;
import com.io7m.rocaro.api.images.RCImageSizeExpressionType;
import com.io7m.rocaro.api.images.RCImageSizeWindowFraction;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

public final class RCArbImageSizeExpression
  extends RCArbAbstractProvider<RCImageSizeExpressionType>
{
  public RCArbImageSizeExpression()
  {
    super(
      RCImageSizeExpressionType.class,
      () -> Arbitraries.oneOf(
        exact(),
        fraction()
      )
    );
  }

  private static Arbitrary<RCImageSizeExpressionType> exact()
  {
    return Combinators.combine(
      Arbitraries.integers().between(0, 1000),
      Arbitraries.integers().between(0, 1000)
    ).as((w, h) -> {
      return new RCImageSizeExact(Vector2I.of(w, h));
    });
  }

  private static Arbitrary<RCImageSizeExpressionType> fraction()
  {
    return Arbitraries.doubles()
      .between(0.0, 32.0)
      .map(RCImageSizeWindowFraction::new);
  }
}
