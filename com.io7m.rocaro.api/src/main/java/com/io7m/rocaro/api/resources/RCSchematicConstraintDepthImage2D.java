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


package com.io7m.rocaro.api.resources;

import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.images.RCImage2DType;

import java.util.Objects;
import java.util.Optional;

/**
 * The base class for depth image constraints.
 *
 * @param <R> The image type
 * @param <S> The image schematic type
 */

public class RCSchematicConstraintDepthImage2D<
  R extends RCImage2DType,
  S extends RCResourceSchematicDepthImage2DType>
  extends RCSchematicConstraintImage2D<R, S>
{
  private final RCDepthComponents depthRequirement;

  /**
   * Construct a constraint.
   *
   * @param inRequiresResourceClass  The required resource superclass
   * @param inRequiresSchematicClass The required schematic superclass
   * @param inRequiresImageLayout    The required image layout
   */

  public RCSchematicConstraintDepthImage2D(
    final Class<? extends R> inRequiresResourceClass,
    final Class<? extends S> inRequiresSchematicClass,
    final Optional<RCGResourceImageLayout> inRequiresImageLayout,
    final RCDepthComponents inDepthRequirement)
  {
    super(
      inRequiresResourceClass,
      inRequiresSchematicClass,
      inRequiresImageLayout,
      false
    );

    this.depthRequirement =
      Objects.requireNonNull(inDepthRequirement, "inDepthRequirement");

    this.addPredicate(
      new RCResourcePredicate<>(
        "The depth attachment must be at least %s."
          .formatted(inDepthRequirement),
        schematic -> {
          final var provided = schematic.depthComponents();
          if (RCDepthComponents.satisfies(inDepthRequirement, provided)) {
            return new RCResourcePredicate.Acceptable(provided.name());
          } else {
            return new RCResourcePredicate.Unacceptable(provided.name());
          }
        }
      )
    );
  }

  public RCDepthComponents depthRequirement()
  {
    return this.depthRequirement;
  }
}
