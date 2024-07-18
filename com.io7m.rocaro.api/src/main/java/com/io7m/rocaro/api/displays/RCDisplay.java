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


package com.io7m.rocaro.api.displays;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;

import java.util.List;
import java.util.Objects;

/**
 * An immutable description of a display.
 *
 * @param isPrimary      The display is the primary display
 * @param id             An opaque display identifier
 * @param name           The display name
 * @param physicalSizeMM The physical size of the display in millimeters
 * @param displayModes   The available display modes
 */

public record RCDisplay(
  boolean isPrimary,
  long id,
  String name,
  Vector2D physicalSizeMM,
  List<RCDisplayMode> displayModes)
{
  /**
   * An immutable description of a display.
   *
   * @param isPrimary      The display is the primary display
   * @param id             An opaque display identifier
   * @param name           The display name
   * @param physicalSizeMM The physical size of the display in millimeters
   * @param displayModes   The available display modes
   */

  public RCDisplay
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(physicalSizeMM, "physicalSizeMM");
    displayModes = List.copyOf(displayModes);
  }

  /**
   * @return The width of the display in millimeters
   */

  public double physicalWidthMM()
  {
    return this.physicalSizeMM.x();
  }

  /**
   * @return The height of the display in millimeters
   */

  public double physicalHeightMM()
  {
    return this.physicalSizeMM.y();
  }
}
