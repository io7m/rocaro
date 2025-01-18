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

package com.io7m.rocaro.rgraphc.internal;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A basic name.
 *
 * @param value The name value
 */

public record RCCName(
  @JsonValue String value)
  implements Comparable<RCCName>
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[A-Za-z0-9_]{1,128}");

  /**
   * A basic name.
   *
   * @param value The name value
   */

  public RCCName
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_NAME.matcher(value).matches()) {
      throw new IllegalArgumentException(
        "Basic names must match %s".formatted(VALID_NAME)
      );
    }
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  @Override
  public int compareTo(
    final RCCName other)
  {
    return this.value.compareTo(other.value);
  }
}
