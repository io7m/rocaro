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


package com.io7m.rocaro.api.images;

import com.io7m.rocaro.api.graph.RCGPortDataConstraintType;

import java.util.Objects;
import java.util.Optional;

/**
 * A constraint on basic color images.
 *
 * @param size     The size expression
 * @param channels The channel constraint
 */

public record RCImageConstraintColorBasic(
  Optional<RCImageSizeExpressionType> size,
  RCImageColorChannels channels)
  implements RCImageConstraintType<RCImageColorBasicType>
{
  /**
   * A constraint on basic color images.
   *
   * @param size     The size expression
   * @param channels The channel constraint
   */

  public RCImageConstraintColorBasic
  {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(channels, "channels");
  }

  @Override
  public Class<RCImageColorBasicType> dataType()
  {
    return RCImageColorBasicType.class;
  }

  @Override
  public boolean isSatisfiedBy(
    final RCGPortDataConstraintType<?> other)
  {
    return switch (other) {
      case final RCImageConstraintType<?> otherImage -> {
        yield switch (otherImage) {
          case final RCImageConstraintColorRenderable c ->
            this.channels.isSatisfiedBy(c.channels());
          case final RCImageConstraintColorBlendable c ->
            this.channels.isSatisfiedBy(c.channels());
          case final RCImageConstraintColorBasic c ->
            this.channels.isSatisfiedBy(c.channels());
          case final RCImageConstraintDepth _,
               final RCImageConstraintDepthStencil _ -> false;
        };
      }
      default -> false;
    };
  }

  @Override
  public String explain()
  {
    final var text = new StringBuilder();
    text.append('(');
    text.append("(image <: ");
    text.append(this.dataType().getSimpleName());
    text.append(") ∧ ");

    if (this.size.isPresent()) {
      text.append(this.size.orElseThrow().explain());
      text.append(" ∧ ");
    }

    text.append(this.channels.explain());
    text.append(')');
    return text.toString();
  }
}
