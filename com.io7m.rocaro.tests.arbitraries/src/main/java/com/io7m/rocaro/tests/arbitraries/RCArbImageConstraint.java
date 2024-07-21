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

import com.io7m.rocaro.api.images.RCImageColorChannels;
import com.io7m.rocaro.api.images.RCImageConstraintColorBasic;
import com.io7m.rocaro.api.images.RCImageConstraintColorBlendable;
import com.io7m.rocaro.api.images.RCImageConstraintColorRenderable;
import com.io7m.rocaro.api.images.RCImageConstraintDepth;
import com.io7m.rocaro.api.images.RCImageConstraintDepthStencil;
import com.io7m.rocaro.api.images.RCImageConstraintType;
import com.io7m.rocaro.api.images.RCImageSizeExpressionType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

public final class RCArbImageConstraint
  extends RCArbAbstractProvider<RCImageConstraintType>
{
  public RCArbImageConstraint()
  {
    super(
      RCImageConstraintType.class,
      () -> Arbitraries.oneOf(
        colorBasic(),
        colorBlendable(),
        colorRenderable(),
        depth(),
        depthStencil()
      )
    );
  }

  private static Arbitrary<RCImageConstraintType<?>> colorBasic()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(RCImageSizeExpressionType.class).optional(),
      Arbitraries.defaultFor(RCImageColorChannels.class)
    ).as(RCImageConstraintColorBasic::new);
  }

  private static Arbitrary<RCImageConstraintType<?>> colorBlendable()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(RCImageSizeExpressionType.class).optional(),
      Arbitraries.defaultFor(RCImageColorChannels.class)
    ).as(RCImageConstraintColorBlendable::new);
  }

  private static Arbitrary<RCImageConstraintType<?>> colorRenderable()
  {
    return Combinators.combine(
      Arbitraries.defaultFor(RCImageSizeExpressionType.class).optional(),
      Arbitraries.defaultFor(RCImageColorChannels.class)
    ).as(RCImageConstraintColorRenderable::new);
  }

  private static Arbitrary<RCImageConstraintType<?>> depth()
  {
    return Arbitraries.defaultFor(RCImageSizeExpressionType.class)
      .optional()
      .map(RCImageConstraintDepth::new);
  }

  private static Arbitrary<RCImageConstraintType<?>> depthStencil()
  {
    return Arbitraries.defaultFor(RCImageSizeExpressionType.class)
      .optional()
      .map(RCImageConstraintDepthStencil::new);
  }
}
