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
import com.io7m.rocaro.api.images.RCImageType;
import com.io7m.rocaro.api.resources.RCResourcePredicate.Acceptable;
import com.io7m.rocaro.api.resources.RCResourcePredicate.Unacceptable;

import java.util.Objects;
import java.util.Optional;

/**
 * The base class for image constraints.
 *
 * @param <R> The image type
 * @param <S> The image schematic type
 */

public non-sealed class RCSchematicConstraintImage2D<
  R extends RCImageType,
  S extends RCResourceSchematicImage2DType>
  extends RCSchematicConstraintAbstract<R, S>
  implements RCSchematicConstraintPrimitiveType<S>
{
  private final Optional<RCGResourceImageLayout> requiresImageLayout;
  private final boolean requiresPresentationSurface;

  /**
   * Construct a constraint.
   *
   * @param inRequiresResourceClass       The required resource superclass
   * @param inRequiresSchematicClass      The required schematic superclass
   * @param inRequiresImageLayout         The required image layout
   * @param inRequiresPresentationSurface {@code true} if the image is a presentation surface
   */

  public RCSchematicConstraintImage2D(
    final Class<? extends R> inRequiresResourceClass,
    final Class<? extends S> inRequiresSchematicClass,
    final Optional<RCGResourceImageLayout> inRequiresImageLayout,
    final boolean inRequiresPresentationSurface)
  {
    super(inRequiresResourceClass, inRequiresSchematicClass);

    this.requiresImageLayout =
      Objects.requireNonNull(inRequiresImageLayout, "requiresImageLayout");
    this.requiresPresentationSurface =
      inRequiresPresentationSurface;

    if (this.requiresPresentationSurface) {
      this.addPredicate(
        new RCResourcePredicate<>(
          "Image must be a presentation surface.",
          schematic -> {
            if (schematic instanceof final RCResourceSchematicImage2DType i) {
              if (i.isPresentationImage()) {
                return new Acceptable("The image is a presentation surface.");
              } else {
                return new Unacceptable(
                  "The image is not a presentation surface.");
              }
            }
            return new Unacceptable("The image is not a presentation surface.");
          }
        )
      );
    }
  }

  /**
   * @return {@code true} if the image must be a presentation surface
   */

  public boolean requiresPresentationSurface()
  {
    return this.requiresPresentationSurface;
  }

  /**
   * @return The required image layout
   */

  public final Optional<RCGResourceImageLayout> requiresImageLayout()
  {
    return this.requiresImageLayout;
  }
}
