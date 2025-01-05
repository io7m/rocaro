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
import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortProducerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Check all applicable ports have assigned resources.
 */

public final class RCGPassCheckPortResourcesAssigned
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  /**
   * Check all applicable ports have assigned resources.
   */

  public RCGPassCheckPortResourcesAssigned()
  {
    super(Set.of(RCGPassCheckPortsConnected.class));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    final var failedPorts =
      new ArrayList<Map.Entry<String, String>>();
    final var graph =
      builder.graph();

    var index = 0;

    for (final var port : graph.vertexSet()) {
      switch (port) {
        case final RCGPortModifierType<?> _ -> {
          // Nothing required
        }
        case final RCGPortProducerType<?> p -> {
          final var resource =
            builder.portResources().get(p);

          if (resource == null) {
            failedPorts.add(
              Map.entry("Port (%d)".formatted(index), p.name().value())
            );
            ++index;
          }
        }
        case final RCGPortConsumerType<?> _ -> {
          // Nothing required
        }
      }
    }

    if (!failedPorts.isEmpty()) {
      throw errorPortResourcesUnassigned(failedPorts);
    }
  }

  private static RCGGraphException errorPortResourcesUnassigned(
    final List<Map.Entry<String, String>> errors)
  {
    return new RCGGraphException(
      "One or more producer ports in the graph do not have assigned resources.",
      errors.stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
      "error-graph-port-unassigned-resource",
      Optional.empty()
    );
  }
}
