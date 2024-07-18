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

import com.io7m.rocaro.api.images.RCImageDepthConstraint;
import com.io7m.rocaro.api.images.RCImageDepthConstraintType;
import com.io7m.rocaro.api.images.RCImageDepthFormatType;
import com.io7m.rocaro.api.images.RCImageFormatCapability;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

import java.util.Optional;
import java.util.Set;

public final class RCArbImageDepthConstraint
  extends RCArbAbstractProvider<RCImageDepthConstraintType>
{
  public RCArbImageDepthConstraint()
  {
    super(
      RCImageDepthConstraintType.class,
      () -> {
        return Combinators.combine(
          format(),
          capabilities()
        ).as(RCImageDepthConstraint::new);
      }
    );
  }

  private static Arbitrary<Set<RCImageFormatCapability>> capabilities()
  {
    return Arbitraries.defaultFor(RCImageFormatCapability.class)
      .set()
      .ofMaxSize(3);
  }

  private static Arbitrary<Optional<RCImageDepthFormatType>> format()
  {
    return Arbitraries.defaultFor(RCImageDepthFormatType.class)
      .optional();
  }
}
