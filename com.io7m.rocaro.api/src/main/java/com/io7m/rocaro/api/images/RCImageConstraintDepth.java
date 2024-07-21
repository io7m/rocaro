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
 * A constraint on a depth image.
 *
 * @param size The size expression
 */

public record RCImageConstraintDepth(
  Optional<RCImageSizeExpressionType> size)
  implements RCImageConstraintType<RCImageDepthType>
{
  private static final RCImageConstraintDepth WINDOW_SIZED_DEPTH =
    new RCImageConstraintDepth(Optional.of(RCImageSizeExpressions.windowSized()));

  /**
   * A constraint on a depth image.
   *
   * @param size The size expression
   */

  public RCImageConstraintDepth
  {
    Objects.requireNonNull(size, "size");
  }

  @Override
  public Class<RCImageDepthType> dataType()
  {
    return RCImageDepthType.class;
  }

  @Override
  public boolean isSatisfiedBy(
    final RCGPortDataConstraintType<?> other)
  {
    return switch (other) {
      case final RCImageConstraintType<?> otherImage -> {
        yield switch (otherImage) {
          case final RCImageConstraintColorRenderable _,
               final RCImageConstraintColorBlendable _,
               final RCImageConstraintColorBasic _ -> false;
          case final RCImageConstraintDepth c ->
            RCImageSizeExpressions.satisfies(this.size, c.size());
          case final RCImageConstraintDepthStencil c ->
            RCImageSizeExpressions.satisfies(this.size, c.size());
        };
      }
      default -> false;
    };
  }

  /**
   * @return A constraint that requires a window-sized depth image
   */

  public static RCImageConstraintDepth requireWindowSizedDepth()
  {
    return WINDOW_SIZED_DEPTH;
  }

  @Override
  public String explain()
  {
    final var text = new StringBuilder();
    text.append('(');
    text.append("(image <: ");
    text.append(this.dataType().getSimpleName());
    text.append(") ∧ ");
    text.append(this.size.orElseThrow().explain());
    text.append(')');
    return text.toString();
  }
}
