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


package com.io7m.rocaro.tests.integration;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenPrimary;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacade;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.windows.RCWindows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("Real-Vulkan-Integration")
public final class RCWindowsIntegrationTest
{
  private RCGLFWFacadeType glfw;
  private RCStrings strings;

  @BeforeEach
  public void setup()
    throws RocaroException
  {
    this.strings =
      new RCStrings(Locale.ROOT);
    this.glfw =
      RCGLFWFacade.get(this.strings);
  }

  @Test
  public void testOpenWindowed()
    throws Exception
  {
    try (final var window =
           RCWindows.create(
             this.strings,
             this.glfw,
             new RCDisplaySelectionWindowed(
               "A Title",
               Vector2I.of(640, 480)))) {
      assertEquals("A Title", window.title());
      assertTrue(window.width() >= 640);
      assertTrue(window.height() >= 480);
    }
  }

  @Test
  public void testOpenFullscreen()
    throws Exception
  {
    try (final var window =
           RCWindows.create(
             this.strings,
             this.glfw,
             new RCDisplaySelectionFullscreenPrimary("A Title"))) {
      assertEquals("A Title", window.title());
      assertTrue(window.width() >= 320);
      assertTrue(window.height() >= 240);
    }
  }
}
