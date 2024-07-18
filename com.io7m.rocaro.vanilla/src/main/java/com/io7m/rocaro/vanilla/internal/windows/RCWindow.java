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
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A non-fullscreen window.
 */

public final class RCWindow
  implements RCWindowType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCWindow.class);

  private final String title;
  private final AtomicBoolean closed;
  private final long address;
  private final RCGLFWFacadeType glfw;

  /**
   * A non-fullscreen window.
   *
   * @param inTitle   The title
   * @param inAddress The address
   * @param inGLFW    The GLFW facade
   */

  public RCWindow(
    final String inTitle,
    final long inAddress,
    final RCGLFWFacadeType inGLFW)
  {
    this.title =
      Objects.requireNonNull(inTitle, "title");
    this.closed =
      new AtomicBoolean(false);
    this.address =
      inAddress;
    this.glfw =
      Objects.requireNonNull(inGLFW, "glfw");
  }

  @Override
  public void close()
  {
    if (this.closed.compareAndSet(false, true)) {
      LOG.debug("Destroying window {}", this);
      this.glfw.windowDestroy(this.address);
    }
  }

  @Override
  public String toString()
  {
    return "[Window 0x%s '%s']"
      .formatted(Long.toUnsignedString(this.address, 16), this.title);
  }

  @Override
  public boolean requiresSurface()
  {
    return true;
  }

  @Override
  public String title()
  {
    return title;
  }

  @Override
  public Vector2I size()
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Window is closed.");
    }

    return this.glfw.windowSize(this.address);
  }

  /**
   * @return The window address
   */

  public long address()
  {
    return address;
  }

  /**
   * @return The GLFW facade
   */

  public RCGLFWFacadeType glfw()
  {
    return glfw;
  }
}
