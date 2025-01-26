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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortConsumer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPrimitiveResourceType;

import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public record RCCPortPrimitiveConsumer(
  @JsonIgnore
  RCTPortConsumer originalPort,
  @JsonProperty("Path")
  RCCPortPath fullPath,
  @JsonIgnore
  RCTPrimitiveResourceType type)
  implements RCCPortPrimitiveType
{
  public RCCPortPrimitiveConsumer
  {
    Objects.requireNonNull(originalPort, "owner");
    Objects.requireNonNull(fullPath, "fullPath");
    Objects.requireNonNull(type, "type");

    Preconditions.checkPrecondition(
      Objects.equals(fullPath.operation(), originalPort.owner().name()),
      "Operation name must match port owner."
    );
  }
}
