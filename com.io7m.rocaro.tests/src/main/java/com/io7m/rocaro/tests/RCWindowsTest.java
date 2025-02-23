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

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3I;
import com.io7m.rocaro.api.displays.RCDisplay;
import com.io7m.rocaro.api.displays.RCDisplayException;
import com.io7m.rocaro.api.displays.RCDisplayMode;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenPrimary;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.windows.RCWindows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DISPLAY_NONE_SUITABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public final class RCWindowsTest
{
  private RCStrings strings;
  private RCGLFWFacadeType glfw;

  @BeforeEach
  public void setup()
  {
    this.strings =
      new RCStrings(Locale.ROOT);
    this.glfw =
      Mockito.mock(RCGLFWFacadeType.class);
  }

  @Test
  public void testDisplaysNoneAvailable()
  {
    when(this.glfw.displays())
      .thenReturn(List.of());

    final var ex =
      assertThrows(RCDisplayException.class, () -> {
        RCWindows.create(
          this.strings,
          this.glfw,
          new RCDisplaySelectionFullscreenPrimary("Any")
        );
      });

    assertEquals(DISPLAY_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  @Test
  public void testDisplayNoModesAvailable()
  {
    when(this.glfw.displays())
      .thenReturn(List.of(
        new RCDisplay(
          true,
          0L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of()
        )
      ));

    final var ex =
      assertThrows(RCDisplayException.class, () -> {
        RCWindows.create(
          this.strings,
          this.glfw,
          new RCDisplaySelectionFullscreenPrimary("Any")
        );
      });

    assertEquals(DISPLAY_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  @Test
  public void testDisplayNoModesAvailableNonPrimary()
  {
    when(this.glfw.displays())
      .thenReturn(List.of(
        new RCDisplay(
          false,
          0L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of()
        ),
        new RCDisplay(
          true,
          1L,
          "Y",
          Vector2D.of(100.0, 100.0),
          List.of()
        )
      ));

    final var ex =
      assertThrows(RCDisplayException.class, () -> {
        RCWindows.create(
          this.strings,
          this.glfw,
          new RCDisplaySelectionFullscreenPrimary("Any")
        );
      });

    assertEquals(DISPLAY_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  @Test
  public void testDisplaysAvailableNonePrimary()
    throws Exception
  {
    when(this.glfw.windowCreateFullscreen(
      anyInt(),
      anyInt(),
      anyString(),
      anyLong()))
      .thenReturn(0x1000_0000_0000_0000L);

    when(this.glfw.windowFramebufferSize(0x1000_0000_0000_0000L))
      .thenReturn(Vector2I.of(600, 400));

    when(this.glfw.displays())
      .thenReturn(List.of(
        new RCDisplay(
          false,
          0L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of(
            new RCDisplayMode(
              Vector2I.of(640, 480),
              Vector3I.of(8, 8, 8),
              60.0
            )
          )
        )
      ));

    try (final var window =
           RCWindows.create(
             this.strings,
             this.glfw,
             new RCDisplaySelectionFullscreenPrimary("Any"))) {
      assertEquals("Any", window.title());
      assertEquals(600, window.width());
      assertEquals(400, window.height());
    }
  }

  @Test
  public void testDisplaysAvailablePrimary()
    throws Exception
  {
    when(this.glfw.windowCreateFullscreen(
      anyInt(),
      anyInt(),
      anyString(),
      anyLong()))
      .thenReturn(0x1000_0000_0000_0000L);

    when(this.glfw.windowFramebufferSize(0x1000_0000_0000_0000L))
      .thenReturn(Vector2I.of(600, 400));

    when(this.glfw.displays())
      .thenReturn(List.of(
        new RCDisplay(
          true,
          0L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of(
            new RCDisplayMode(
              Vector2I.of(640, 480),
              Vector3I.of(8, 8, 8),
              60.0
            )
          )
        )
      ));

    try (final var window =
           RCWindows.create(
             this.strings,
             this.glfw,
             new RCDisplaySelectionFullscreenPrimary("Any"))) {
      assertEquals("Any", window.title());
      assertEquals(600, window.width());
      assertEquals(400, window.height());
    }
  }

  @Test
  public void testDisplaysAvailableSecondPrimary()
    throws Exception
  {
    when(this.glfw.windowCreateFullscreen(
      anyInt(),
      anyInt(),
      anyString(),
      anyLong()))
      .thenReturn(0x1000_0000_0000_0000L);

    when(this.glfw.windowFramebufferSize(0x1000_0000_0000_0000L))
      .thenReturn(Vector2I.of(600, 400));

    when(this.glfw.displays())
      .thenReturn(List.of(
        new RCDisplay(
          false,
          0L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of(
            new RCDisplayMode(
              Vector2I.of(640, 480),
              Vector3I.of(8, 8, 8),
              60.0
            )
          )
        ),
        new RCDisplay(
          true,
          1L,
          "X",
          Vector2D.of(100.0, 100.0),
          List.of(
            new RCDisplayMode(
              Vector2I.of(800, 600),
              Vector3I.of(8, 8, 8),
              60.0
            )
          )
        )
      ));

    try (final var window =
           RCWindows.create(
             this.strings,
             this.glfw,
             new RCDisplaySelectionFullscreenPrimary("Any"))) {
      assertEquals("Any", window.title());
      assertEquals(600, window.width());
      assertEquals(400, window.height());
    }
  }
}
