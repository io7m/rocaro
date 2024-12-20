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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderFrameImageType;

import java.util.Objects;

/**
 * A resource representing the frame image.
 */

public final class RCGResourcePlaceholderFrameImage
  implements RCGResourcePlaceholderFrameImageType
{
  private final RCGResourceName name;

  /**
   * A resource representing the frame image.
   *
   * @param inName The resource name
   */

  public RCGResourcePlaceholderFrameImage(
    final RCGResourceName inName)
  {
    this.name = Objects.requireNonNull(inName, "name");
  }

  @Override
  public String toString()
  {
    return "[RCGResourcePlaceholderFrameImage %s]".formatted(this.name);
  }

  @Override
  public RCGResourceName name()
  {
    return this.name;
  }
}
