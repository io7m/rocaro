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

import java.util.List;

/**
 * A constraint on a resource schematic.
 *
 * @param <S> The type of resource schematic
 */

public sealed interface RCSchematicConstraintType<S extends RCResourceSchematicType>
  permits RCSchematicConstraintCompositeType,
  RCSchematicConstraintPrimitiveType
{
  /**
   * @return The resource class
   */

  Class<?> requiresResourceClass();

  /**
   * @return The resource schematic class
   */

  Class<?> requiresSchematicClass();

  /**
   * @return The resource predicates
   */

  List<RCResourcePredicate<S>> predicates();

  /**
   * Check the given schematic against this constraint.
   *
   * @param schematic The schematic
   *
   * @throws RCConstraintException On checking failures
   */

  void checkSchematic(
    RCResourceSchematicType schematic)
    throws RCConstraintException;
}
