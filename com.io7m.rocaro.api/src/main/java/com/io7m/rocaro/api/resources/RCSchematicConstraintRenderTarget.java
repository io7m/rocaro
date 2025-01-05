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

import com.io7m.rocaro.api.graph.RCGResourceSubname;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.render_targets.RCRenderTargetType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The base class for render target constraints.
 *
 * @param <R> The render target type
 * @param <S> The render target schematic type
 */

public class RCSchematicConstraintRenderTarget<
  R extends RCRenderTargetType,
  S extends RCResourceSchematicRenderTargetType>
  extends RCSchematicConstraintAbstract<R, S>
  implements RCSchematicConstraintCompositeType<S>
{
  private final List<
    RCSchematicConstraintImage2D<RCImage2DType, RCResourceSchematicImage2DType>>
    colorAttachmentConstraints;
  private final Optional<
    RCSchematicConstraintDepthImage2D<
      RCImage2DType,
      RCResourceSchematicDepthImage2DType>> depthConstraint;

  private final Map<RCGResourceSubname, RCSchematicConstraintPrimitiveType<?>> allConstraints;

  /**
   * Construct a constraint.
   *
   * @param inRequiresResourceClass      The required resource superclass
   * @param inRequiresSchematicClass     The required schematic superclass
   * @param inColorAttachmentConstraints The constraints on the color attachments
   * @param inRequiresDepth              The required depth
   */

  public RCSchematicConstraintRenderTarget(
    final Class<? extends R> inRequiresResourceClass,
    final Class<? extends S> inRequiresSchematicClass,
    final List<RCSchematicConstraintImage2D<RCImage2DType, RCResourceSchematicImage2DType>> inColorAttachmentConstraints,
    final Optional<RCSchematicConstraintDepthImage2D<RCImage2DType, RCResourceSchematicDepthImage2DType>> inRequiresDepth)
  {
    super(inRequiresResourceClass, inRequiresSchematicClass);

    this.depthConstraint =
      Objects.requireNonNull(inRequiresDepth, "inRequiresDepth");
    this.colorAttachmentConstraints =
      List.copyOf(inColorAttachmentConstraints);

    final var constraintMap =
      new HashMap<RCGResourceSubname, RCSchematicConstraintPrimitiveType<?>>();

    for (int index = 0; index < this.colorAttachmentConstraints.size(); ++index) {
      final var constraint =
        this.colorAttachmentConstraints.get(index);

      constraintMap.put(
        new RCGResourceSubname("Color%d".formatted(index)),
        constraint
      );

      for (final var imagePredicate : constraint.predicates()) {
        final int imageIndex = index;
        this.addPredicate(
          new RCResourcePredicate<>(
            imagePredicate.description(),
            renderTarget -> {
              final var attachment =
                renderTarget.colorAttachments()
                  .get(imageIndex);

              return imagePredicate.check()
                .check(attachment);
            }
          )
        );
      }
    }

    this.depthConstraint.ifPresent(constraint -> {
      constraintMap.put(
        new RCGResourceSubname("Depth"),
        constraint
      );
    });

    this.allConstraints = Map.copyOf(constraintMap);
  }

  @Override
  public Map<RCGResourceSubname, RCSchematicConstraintPrimitiveType<?>> primitiveConstraints()
  {
    return this.allConstraints;
  }
}
