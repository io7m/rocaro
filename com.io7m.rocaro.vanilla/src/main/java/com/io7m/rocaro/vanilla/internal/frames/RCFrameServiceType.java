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


package com.io7m.rocaro.vanilla.internal.frames;

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.services.RCServiceRendererScopedType;

/**
 * A service that exposes information about the current frame.
 */

public interface RCFrameServiceType
  extends RPServiceType, RCServiceRendererScopedType
{
  /**
   * A property that exposes information about the current frame, and is
   * updated only when a new frame begins.
   *
   * @return The current frame information
   */

  AttributeReadableType<RCFrameInformation> frameInformation();

  /**
   * Set the new frame information.
   *
   * @param frameInformation The new frame
   */

  void beginNewFrame(
    RCFrameInformation frameInformation);
}
