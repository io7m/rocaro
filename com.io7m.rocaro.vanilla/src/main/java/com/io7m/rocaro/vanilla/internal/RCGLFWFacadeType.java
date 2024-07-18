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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.displays.RCDisplay;

import java.util.List;
import java.util.SortedSet;

/**
 * A facade to an underlying GLFW implementation.
 */

public interface RCGLFWFacadeType
{
  /**
   * @return The current list of displays
   */

  List<RCDisplay> displays();

  /**
   * Create a non-fullscreen window.
   *
   * @param width  The width
   * @param height The height
   * @param title  The title
   *
   * @return A window
   */

  long windowCreateWindowed(
    int width,
    int height,
    String title);

  /**
   * Create a fullscreen window.
   *
   * @param width     The width
   * @param height    The height
   * @param title     The title
   * @param displayID The display
   *
   * @return A window
   */

  long windowCreateFullscreen(
    int width,
    int height,
    String title,
    long displayID);

  /**
   * @return The Vulkan extensions required by the window system
   */

  SortedSet<String> requiredExtensions();

  /**
   * Destroy a window.
   *
   * @param address The window address
   */

  void windowDestroy(
    long address);

  /**
   * @param address The window address
   *
   * @return The size of the window
   */

  Vector2I windowSize(
    long address);
}
