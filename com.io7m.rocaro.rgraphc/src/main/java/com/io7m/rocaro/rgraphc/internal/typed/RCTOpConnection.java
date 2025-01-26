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


package com.io7m.rocaro.rgraphc.internal.typed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.Comparator;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public record RCTOpConnection(
  @JsonIgnore
  RCTOperationDeclaration source,
  @JsonIgnore
  RCTOperationDeclaration target)
  implements Comparable<RCTOpConnection>
{
  public RCTOpConnection
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
  }

  private RCCName sourceName()
  {
    return this.source.name();
  }

  private RCCName targetName()
  {
    return this.target.name();
  }

  @Override
  public int compareTo(
    final RCTOpConnection other)
  {
    return Comparator.comparing(RCTOpConnection::sourceName)
      .thenComparing(RCTOpConnection::targetName)
      .compare(this, other);
  }
}
