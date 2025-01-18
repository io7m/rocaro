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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.Comparator;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public record RCTPortConnection(
  @JsonIgnore
  RCTPortSourceType source,
  @JsonIgnore
  RCTPortTargetType target)
  implements Comparable<RCTPortConnection>
{
  public RCTPortConnection
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
  }

  @JsonProperty("SourceOperation")
  public RCCName sourceOperation()
  {
    return this.source.owner().name();
  }

  @JsonProperty("SourcePort")
  public RCCName sourcePort()
  {
    return this.source.name();
  }

  @JsonProperty("TargetOperation")
  public RCCName targetOperation()
  {
    return this.target.owner().name();
  }

  @JsonProperty("TargetPort")
  public RCCName targetPort()
  {
    return this.target.name();
  }

  @Override
  public int compareTo(
    final RCTPortConnection other)
  {
    return Comparator.comparing(RCTPortConnection::sourceOperation)
      .thenComparing(RCTPortConnection::sourcePort)
      .thenComparing(RCTPortConnection::targetOperation)
      .thenComparing(RCTPortConnection::targetPort)
      .compare(this, other);
  }
}
