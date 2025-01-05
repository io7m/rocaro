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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract constraint implementation.
 *
 * @param <R> The resource type
 * @param <S> The resource schematic type
 */

public abstract class RCSchematicConstraintAbstract<
  R extends RCResourceType,
  S extends RCResourceSchematicType>
{
  private final List<RCResourcePredicate<S>> predicatesRead;
  private final List<RCResourcePredicate<S>> predicates;
  private final Class<? extends R> requiresResourceClass;
  private final Class<? extends S> requiresSchematicClass;

  /**
   * Construct a constraint.
   *
   * @param inRequiresResourceClass  The required resource class
   * @param inRequiresSchematicClass The required schematic class
   */

  public RCSchematicConstraintAbstract(
    final Class<? extends R> inRequiresResourceClass,
    final Class<? extends S> inRequiresSchematicClass)
  {
    this.requiresResourceClass =
      Objects.requireNonNull(
        inRequiresResourceClass, "requiresResourceClass");
    this.requiresSchematicClass =
      Objects.requireNonNull(
        inRequiresSchematicClass, "requiresSchematicClass");

    this.predicates =
      new ArrayList<>();
    this.predicatesRead =
      Collections.unmodifiableList(this.predicates);
  }

  /**
   * @return The resource class
   */

  public final Class<? extends R> requiresResourceClass()
  {
    return this.requiresResourceClass;
  }

  /**
   * @return The resource schematic class
   */

  public final Class<? extends S> requiresSchematicClass()
  {
    return this.requiresSchematicClass;
  }

  /**
   * Add a predicate.
   *
   * @param predicate The predicate
   */

  protected final void addPredicate(
    final RCResourcePredicate<S> predicate)
  {
    this.predicates.add(
      Objects.requireNonNull(predicate, "predicate")
    );
  }

  /**
   * @return The resource predicates
   */

  public final List<RCResourcePredicate<S>> predicates()
  {
    return this.predicatesRead;
  }

  /**
   * @param schematic The schematic
   *
   * @throws RCConstraintException On errors
   * @see RCSchematicConstraintType#checkSchematic(RCResourceSchematicType)
   */

  public final void checkSchematic(
    final RCResourceSchematicType schematic)
    throws RCConstraintException
  {
    final Class<?> requiredSuperclass =
      this.requiresSchematicClass();

    if (!requiredSuperclass.isAssignableFrom(schematic.getClass())) {
      throw errorTypeIncompatibleSchematic(schematic, requiredSuperclass);
    }

    final var schematicTyped =
      (S) schematic;

    final List<RCResourcePredicate<S>> predicateList =
      this.predicates();

    int failures = 0;
    final var failureEntries =
      new HashMap<String, String>();

    for (final RCResourcePredicate<S> predicate : predicateList) {
      final RCResourcePredicate.CheckFunctionType<S> check = predicate.check();

      switch (check.check(schematicTyped)) {
        case final RCResourcePredicate.Acceptable _ -> {
          // Nothing to do.
        }
        case final RCResourcePredicate.Unacceptable unacceptable -> {
          failureEntries.put(
            "Required [%d]".formatted(failures),
            predicate.description()
          );
          failureEntries.put(
            "Provided [%d]".formatted(failures),
            unacceptable.value()
          );
          ++failures;
        }
      }
    }

    if (!failureEntries.isEmpty()) {
      throw errorTypeIncompatibleRequirements(
        failureEntries
      );
    }
  }

  private static RCConstraintException errorTypeIncompatibleRequirements(
    final Map<String, String> failures)
  {
    return new RCConstraintException(
      "The resource does not meet one or more of the specified requirements.",
      Map.copyOf(failures),
      "error-type-incompatible",
      Optional.empty()
    );
  }

  private static RCConstraintException errorTypeIncompatibleSchematic(
    final RCResourceSchematicType schematic,
    final Class<?> requiredSuperclass)
  {
    return new RCConstraintException(
      "The resource is type-incompatible with the given superclass constraint.",
      Map.ofEntries(
        Map.entry(
          "Type (Required)",
          requiredSuperclass.getName()
        ),
        Map.entry(
          "Type (Provided)",
          schematic.getClass().getName()
        )
      ),
      "error-type-incompatible",
      Optional.empty()
    );
  }
}
