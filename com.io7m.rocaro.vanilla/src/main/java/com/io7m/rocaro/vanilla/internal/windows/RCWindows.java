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


package com.io7m.rocaro.vanilla.internal.windows;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.displays.RCDisplay;
import com.io7m.rocaro.api.displays.RCDisplayException;
import com.io7m.rocaro.api.displays.RCDisplayMode;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenExact;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenPrimary;
import com.io7m.rocaro.api.displays.RCDisplaySelectionOffscreen;
import com.io7m.rocaro.api.displays.RCDisplaySelectionPreciseType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DISPLAY_NONE_SUITABLE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DISPLAY_WINDOW_CREATION;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_DISPLAY_EXPLICIT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_DISPLAY_NO_SUITABLE_PRIMARY_FULLSCREEN;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_WINDOW_CREATION_FAILED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.WINDOW_HEIGHT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.WINDOW_WIDTH;

/**
 * Functions to allocate windows.
 */

public final class RCWindows
{
  private RCWindows()
  {

  }

  /**
   * Create a new window.
   *
   * @param strings          The string resources
   * @param glfw             The GLFW facade
   * @param displaySelection The display selection method
   *
   * @return A new window
   *
   * @throws RCDisplayException On errors
   */

  public static RCWindowType create(
    final RCStrings strings,
    final RCGLFWFacadeType glfw,
    final RCDisplaySelectionType displaySelection)
    throws RCDisplayException
  {
    final var displays = glfw.displays();
    return switch (displaySelection) {
      case final RCDisplaySelectionFullscreenPrimary fullscreenPrimary -> {
        yield createWindowFullscreenPrimary(
          strings,
          glfw,
          displays,
          fullscreenPrimary
        );
      }
      case final RCDisplaySelectionPreciseType precise -> {
        yield switch (precise) {
          case final RCDisplaySelectionFullscreenExact fullscreenExact ->
            createWindowFullscreenExact(strings, glfw, fullscreenExact);
          case final RCDisplaySelectionOffscreen offscreen ->
            createWindowOffscreen(strings, glfw, displays, offscreen);
          case final RCDisplaySelectionWindowed windowed ->
            createWindowWindows(strings, glfw, windowed);
        };
      }
    };
  }

  private static RCWindowType createWindowWindows(
    final RCStrings strings,
    final RCGLFWFacadeType glfw,
    final RCDisplaySelectionWindowed windowed)
    throws RCDisplayException
  {
    final long window =
      glfw.windowCreateWindowed(
        windowed.size().x(),
        windowed.size().y(),
        windowed.title()
      );

    if (window == 0L) {
      throw errorWindowCreationFailed(strings, windowed.size());
    }

    return new RCWindow(
      windowed.title(),
      window,
      glfw
    );
  }

  private static RCWindowType createWindowOffscreen(
    final RCStrings strings,
    final RCGLFWFacadeType glfw,
    final List<RCDisplay> displays,
    final RCDisplaySelectionOffscreen offscreen)
    throws RCDisplayException
  {
    throw new UnimplementedCodeException();
  }

  private static RCWindowType createWindowFullscreenExact(
    final RCStrings strings,
    final RCGLFWFacadeType glfw,
    final RCDisplaySelectionFullscreenExact fullscreenExact)
    throws RCDisplayException
  {
    final long window =
      glfw.windowCreateFullscreen(
        fullscreenExact.mode().widthPixels(),
        fullscreenExact.mode().heightPixels(),
        fullscreenExact.title(),
        fullscreenExact.display().id()
      );

    if (window == 0L) {
      throw errorWindowCreationFailed(strings, fullscreenExact.mode());
    }

    return new RCWindowFullscreen(
      fullscreenExact.title(),
      window,
      glfw
    );
  }

  private static RCWindowType createWindowFullscreenPrimary(
    final RCStrings strings,
    final RCGLFWFacadeType glfw,
    final List<RCDisplay> displays,
    final RCDisplaySelectionFullscreenPrimary fullscreenPrimary)
    throws RCDisplayException
  {
    for (final var display : displays) {
      if (!display.isPrimary()) {
        continue;
      }

      final var bestMode =
        display.displayModes()
          .stream()
          .max(RCDisplayMode::compareTo);

      if (bestMode.isEmpty()) {
        continue;
      }

      return createWindowFullscreenExact(
        strings,
        glfw,
        new RCDisplaySelectionFullscreenExact(
          fullscreenPrimary.title(),
          display,
          bestMode.get()
        )
      );
    }

    for (final var display : displays) {
      final var bestMode =
        display.displayModes()
          .stream()
          .max(RCDisplayMode::compareTo);

      if (bestMode.isEmpty()) {
        continue;
      }

      return createWindowFullscreenExact(
        strings,
        glfw,
        new RCDisplaySelectionFullscreenExact(
          fullscreenPrimary.title(),
          display,
          bestMode.get()
        )
      );
    }

    throw errorNoSuitableDisplayFullscreenPrimary(strings);
  }

  private static RCDisplayException errorNoSuitableDisplayFullscreenPrimary(
    final RCStrings strings)
  {
    return new RCDisplayException(
      strings.format(ERROR_DISPLAY_NO_SUITABLE_PRIMARY_FULLSCREEN),
      Map.of(),
      DISPLAY_NONE_SUITABLE.codeName(),
      Optional.of(strings.format(ERROR_DISPLAY_EXPLICIT))
    );
  }

  private static RCDisplayException errorWindowCreationFailed(
    final RCStrings strings,
    final RCDisplayMode mode)
  {
    return new RCDisplayException(
      strings.format(ERROR_WINDOW_CREATION_FAILED),
      Map.ofEntries(
        Map.entry(
          strings.format(WINDOW_WIDTH),
          Integer.toUnsignedString(mode.widthPixels())
        ),
        Map.entry(
          strings.format(WINDOW_HEIGHT),
          Integer.toUnsignedString(mode.heightPixels())
        )
      ),
      DISPLAY_WINDOW_CREATION.codeName(),
      Optional.of(strings.format(ERROR_DISPLAY_EXPLICIT))
    );
  }

  private static RCDisplayException errorWindowCreationFailed(
    final RCStrings strings,
    final Vector2I size)
  {
    return new RCDisplayException(
      strings.format(ERROR_WINDOW_CREATION_FAILED),
      Map.ofEntries(
        Map.entry(
          strings.format(WINDOW_WIDTH),
          Integer.toUnsignedString(size.x())
        ),
        Map.entry(
          strings.format(WINDOW_HEIGHT),
          Integer.toUnsignedString(size.y())
        )
      ),
      DISPLAY_WINDOW_CREATION.codeName(),
      Optional.of(strings.format(ERROR_DISPLAY_EXPLICIT))
    );
  }
}
