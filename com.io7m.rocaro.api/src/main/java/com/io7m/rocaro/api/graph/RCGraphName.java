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


package com.io7m.rocaro.api.graph;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The name of a render graph.
 *
 * @param value The name value
 */

public record RCGraphName(
  String value)
  implements Comparable<RCGraphName>
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[A-Za-z0-9_]{1,128}");

  /**
   * The name of a render graph.
   *
   * @param value The name value
   */

  public RCGraphName
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_NAME.matcher(value).matches()) {
      throw new IllegalArgumentException(
        "Graph names must match %s".formatted(VALID_NAME)
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
    final RCGraphName other)
  {
    return Comparator.comparing(RCGraphName::value)
      .compare(this, other);
  }
}
