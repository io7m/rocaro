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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Check all ports are connected.
 */

public final class RCGPassCheckPortsConnected
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  /**
   * Check all ports are connected.
   */

  public RCGPassCheckPortsConnected()
  {
    super(Set.of(RCGPassCheckNonEmpty.class));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    int index = 0;

    final var failedPorts =
      new ArrayList<Map.Entry<String, String>>();
    final var graph =
      builder.graph();

    for (final var port : graph.vertexSet()) {
      if (graph.degreeOf(port) == 0) {
        failedPorts.add(
          Map.entry(
            "Port (%d)".formatted(index),
            port.name().value()
          )
        );
        ++index;
      }
    }

    if (!failedPorts.isEmpty()) {
      throw errorPortsUnconnected(failedPorts);
    }
  }

  private static RCGGraphException errorPortsUnconnected(
    final List<Map.Entry<String, String>> errors)
  {
    return new RCGGraphException(
      "One or more ports in the graph are not connected.",
      errors.stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
      "error-graph-ports-unconnected",
      Optional.empty()
    );
  }
}
