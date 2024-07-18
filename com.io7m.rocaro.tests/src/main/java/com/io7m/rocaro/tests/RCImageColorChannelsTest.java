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

import com.io7m.rocaro.api.images.RCImageColorChannels;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_ABGR;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_ARGB;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_BGRA;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_R;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RG;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RGB;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RGBA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RCImageColorChannelsTest
{
  private record IsSuperset(
    RCImageColorChannels c0,
    RCImageColorChannels c1)
  {

  }

  @Property
  public void testSupersetReflexive(
    final @ForAll RCImageColorChannels channels)
  {
    assertTrue(channels.isSupersetOf(channels));
  }

  @TestFactory
  public Stream<DynamicTest> testSupersetOf()
  {
    return Stream.of(
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_R),

      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_RG),

      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_RGB),

      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_RGBA, COLOR_CHANNELS_BGRA),

      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_ABGR, COLOR_CHANNELS_BGRA),

      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_ARGB, COLOR_CHANNELS_BGRA),

      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_R),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_BGRA, COLOR_CHANNELS_BGRA)
    ).map(isSuperset -> {
      return DynamicTest.dynamicTest("testSupersetOf_" + isSuperset, () -> {
        assertTrue(
          isSuperset.c0.isSupersetOf(isSuperset.c1),
          "%s is a superset of %s".formatted(isSuperset.c0, isSuperset.c1)
        );
      });
    });
  }

  @TestFactory
  public Stream<DynamicTest> testNotSupersetOf()
  {
    return Stream.of(
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_RG),
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_R, COLOR_CHANNELS_BGRA),

      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_RGB),
      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_RG, COLOR_CHANNELS_BGRA),

      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_RGBA),
      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_ABGR),
      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_ARGB),
      new IsSuperset(COLOR_CHANNELS_RGB, COLOR_CHANNELS_BGRA)
    ).map(isSuperset -> {
      return DynamicTest.dynamicTest("testNotSupersetOf" + isSuperset, () -> {
        assertFalse(
          isSuperset.c0.isSupersetOf(isSuperset.c1),
          "%s is not a superset of %s".formatted(isSuperset.c0, isSuperset.c1)
        );
      });
    });
  }
}
