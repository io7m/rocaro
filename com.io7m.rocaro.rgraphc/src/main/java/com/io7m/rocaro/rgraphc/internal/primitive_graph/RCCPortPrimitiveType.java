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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.access_set.RCTAccessSetCompositeType;
import com.io7m.rocaro.rgraphc.internal.access_set.RCTAccessSetSingletonType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPrimitiveResourceType;

import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public sealed interface RCCPortPrimitiveType
  permits RCCPortPrimitiveConsumer,
  RCCPortPrimitiveModifier,
  RCCPortPrimitiveProducer
{
  default RCTOperationDeclaration owner()
  {
    return this.originalPort().owner();
  }

  RCCPortPath fullPath();

  RCTPortType originalPort();

  RCTPrimitiveResourceType type();

  @JsonProperty("Reads")
  @JsonInclude(NON_EMPTY)
  default Set<RCGCommandPipelineStage> reads()
  {
    return switch (this.originalPort().accessSet()) {
      case final RCTAccessSetCompositeType c -> {
        yield c.readsFor(this.fullPath().path());
      }
      case final RCTAccessSetSingletonType s -> {
        yield s.reads();
      }
    };
  }

  @JsonProperty("Writes")
  @JsonInclude(NON_EMPTY)
  default Set<RCGCommandPipelineStage> writes()
  {
    return switch (this.originalPort().accessSet()) {
      case final RCTAccessSetCompositeType c -> {
        yield c.writesFor(this.fullPath().path());
      }
      case final RCTAccessSetSingletonType s -> {
        yield s.writes();
      }
    };
  }

  @JsonProperty("EnsuresImageLayout")
  @JsonInclude(NON_ABSENT)
  default Optional<RCGResourceImageLayout> ensuresImageLayout()
  {
    return switch (this.originalPort().accessSet()) {
      case final RCTAccessSetCompositeType c -> {
        yield c.ensuresImageLayoutFor(this.fullPath().path());
      }
      case final RCTAccessSetSingletonType s -> {
        yield s.ensuresImageLayout();
      }
    };
  }

  @JsonProperty("RequiresImageLayout")
  @JsonInclude(NON_ABSENT)
  default Optional<RCGResourceImageLayout> requiresImageLayout()
  {
    return switch (this.originalPort().accessSet()) {
      case final RCTAccessSetCompositeType c -> {
        yield c.requiresImageLayoutFor(this.fullPath().path());
      }
      case final RCTAccessSetSingletonType s -> {
        yield s.requiresImageLayout();
      }
    };
  }
}
