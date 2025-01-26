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


package com.io7m.rocaro.rgraphc.internal.primitive_graph;

import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPath;

import java.util.Comparator;
import java.util.Objects;

public record RCCPortPath(
  RCCName operation,
  RCCPath path)
  implements Comparable<RCCPortPath>
{
  public RCCPortPath
  {
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(path, "path");
  }

  @Override
  public String toString()
  {
    return "%s:%s".formatted(this.operation, this.path);
  }

  @Override
  public int compareTo(
    final RCCPortPath other)
  {
    return Comparator.comparing(RCCPortPath::operation)
      .thenComparing(RCCPortPath::path)
      .compare(this, other);
  }
}
