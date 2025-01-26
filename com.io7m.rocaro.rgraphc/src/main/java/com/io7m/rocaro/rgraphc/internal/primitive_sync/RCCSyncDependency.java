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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Comparator;
import java.util.Objects;

/**
 * An indication that the source operation <i>happens before</i> the target
 * operation.
 *
 * @param source The source operation
 * @param target The target operation
 */

@JsonPropertyOrder(alphabetic = true)
public record RCCSyncDependency(
  @JsonIgnore
  RCCCommandType source,
  @JsonIgnore
  RCCCommandType target)
  implements Comparable<RCCSyncDependency>
{
  /**
   * An indication that the source operation <i>happens before</i> the target
   * operation.
   *
   * @param source The source operation
   * @param target The target operation
   */

  public RCCSyncDependency
  {
    Objects.requireNonNull(source, "before");
    Objects.requireNonNull(target, "after");
  }

  @Override
  public String toString()
  {
    return "[%s → %s]".formatted(this.source, this.target);
  }

  @JsonProperty("Source")
  private String sourceIdText()
  {
    return Long.toUnsignedString(this.sourceId());
  }

  @JsonProperty("Target")
  private String targetIdText()
  {
    return Long.toUnsignedString(this.targetId());
  }

  private long sourceId()
  {
    return this.source.commandId();
  }

  private long targetId()
  {
    return this.target.commandId();
  }

  @Override
  public int compareTo(
    final RCCSyncDependency other)
  {
    return Comparator.comparing(RCCSyncDependency::sourceId)
      .thenComparing(RCCSyncDependency::targetId)
      .compare(this, other);
  }
}
