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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.rocaro.api.graph.RCGGraphException;

import java.util.Map;
import java.util.Optional;

/**
 * Check resource types for the graph.
 */

public final class RCGPassCheckTypes
  implements RCGGraphPassType
{
  /**
   * Check resource types for the graph.
   */

  public RCGPassCheckTypes()
  {

  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    for (final var entry : builder.portResourcesTracked().entrySet()) {
      final var port =
        entry.getKey();
      final var resource =
        entry.getValue();
      final var requiredType =
        port.type();
      final var providedType =
        resource.getClass();

      if (!requiredType.isAssignableFrom(providedType)) {
        throw new RCGGraphException(
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
  }
}
