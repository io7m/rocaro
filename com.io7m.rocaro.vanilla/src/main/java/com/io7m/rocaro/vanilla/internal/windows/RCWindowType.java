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
import com.io7m.rocaro.api.RocaroException;

/**
 * The type of windows.
 */

public sealed interface RCWindowType
  extends AutoCloseable
  permits RCWindow,
  RCWindowFullscreen,
  RCWindowOffscreen
{
  /**
   * @return The window title
   */

  String title();

  /**
   * @return {@code true} iff this type of window requires a rendering surface
   */

  boolean requiresSurface();

  /**
   * @return The current size of the window
   */

  Vector2I size();

  /**
   * @return The current width of the window
   */

  default int width()
  {
    return this.size().x();
  }

  /**
   * @return The current height of the window
   */

  default int height()
  {
    return this.size().y();
  }

  @Override
  void close()
    throws RocaroException;
}
