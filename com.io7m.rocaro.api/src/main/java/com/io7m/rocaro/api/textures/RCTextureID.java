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


package com.io7m.rocaro.api.textures;

import java.util.Objects;
import java.util.UUID;

/**
 * A unique identifier for a texture.
 *
 * @param value The value
 */

public record RCTextureID(UUID value)
  implements Comparable<RCTextureID>
{
  /**
   * A unique identifier for a texture.
   *
   * @param value The value
   */

  public RCTextureID
  {
    Objects.requireNonNull(value, "value");
  }

  @Override
  public String toString()
  {
    return "[RCBufferID %s]".formatted(this.value);
  }

  @Override
  public int compareTo(
    final RCTextureID o)
  {
    return this.value.compareTo(o.value);
  }
}
