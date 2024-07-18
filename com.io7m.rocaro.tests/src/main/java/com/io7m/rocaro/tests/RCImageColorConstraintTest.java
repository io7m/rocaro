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


package com.io7m.rocaro.tests;

import com.io7m.rocaro.api.images.RCImageColorConstraint;
import com.io7m.rocaro.api.images.RCImageColorConstraintType;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.images.RCImageColorFormat.COLOR_FORMAT_R8_UNSIGNED_NORMALIZED;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.RENDERING_BLENDING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RCImageColorConstraintTest
{
  @Property
  public void testReflexive(
    final @ForAll RCImageColorConstraintType c)
  {
    assertTrue(c.isSatisfiedBy(c));
  }

  @Test
  public void testSatisfiedBy0()
  {
    final var c0 =
      new RCImageColorConstraint(
        Optional.empty(),
        Optional.empty(),
        Set.of()
      );

    final var c1 =
      new RCImageColorConstraint(
        Optional.empty(),
        Optional.empty(),
        Set.of()
      );

    assertTrue(c1.isSatisfiedBy(c0));
    assertTrue(c0.isSatisfiedBy(c1));
  }

  @Test
  public void testSatisfiedBy1()
  {
    final var c0 =
      new RCImageColorConstraint(
        Optional.of(COLOR_FORMAT_R8_UNSIGNED_NORMALIZED),
        Optional.empty(),
        Set.of()
      );

    final var c1 =
      new RCImageColorConstraint(
        Optional.empty(),
        Optional.empty(),
        Set.of(RENDERING_BLENDING)
      );

    assertFalse(c0.isSatisfiedBy(c1));
    assertTrue(c1.isSatisfiedBy(c0));
  }
}
