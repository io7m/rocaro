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


package com.io7m.rocaro.api.graph;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Functions to determine if ports are compatible.
 */

public final class RCGResourceCompatibility
{
  private RCGResourceCompatibility()
  {

  }

  /**
   * Check if the given port can be assigned a resource of the given type.
   *
   * @param port     The port
   * @param resource The resource
   *
   * @throws RCGGraphException On incompatibility
   */

  public static void checkCompatibility(
    final RCGPortType port,
    final RCGResourcePlaceholderType resource)
    throws RCGGraphException
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(resource, "resource");

    final var providedType =
      resource.getClass();

    switch (port) {
      case final RCGPortModifierType m -> {
        final var requiredType = m.typeConsumed().resourceType();
        checkTypeCompatible(port, resource, requiredType, providedType);
      }

      case final RCGPortConsumerType c -> {
        final var requiredType = c.typeConsumed().resourceType();
        checkTypeCompatible(port, resource, requiredType, providedType);
      }

      case final RCGPortProducerType p -> {
        final var requiredType = p.typeProduced().resourceType();
        checkTypeCompatible(port, resource, requiredType, providedType);
      }
    }
  }

  private static void checkTypeCompatible(
    final RCGPortType port,
    final RCGResourcePlaceholderType resource,
    final Class<? extends RCGResourcePlaceholderType> requiredType,
    final Class<? extends RCGResourcePlaceholderType> providedType)
    throws RCGGraphException
  {
    if (!requiredType.isAssignableFrom(providedType)) {
      throw errorTypeIncompatible(
        port,
        resource,
        requiredType,
        providedType
      );
    }
  }

  private static RCGGraphException errorTypeIncompatible(
    final RCGPortType port,
    final RCGResourcePlaceholderType resource,
    final Class<? extends RCGResourcePlaceholderType> requiredType,
    final Class<? extends RCGResourcePlaceholderType> providedType)
  {
    return new RCGGraphException(
      "The assigned resource is type-incompatible with the given port.",
      Map.ofEntries(
        Map.entry("Operation", port.owner().name().value()),
        Map.entry("Port", port.name().value()),
        Map.entry("Resource", resource.name().value()),
        Map.entry("Type (Expected)", requiredType.getName()),
        Map.entry("Type (Provided)", providedType.getName())
      ),
      "error-graph-type-incompatible",
      Optional.empty()
    );
  }
}
