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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.rocaro.api.graph2.RCGGraphType;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceName;
import com.io7m.rocaro.api.graph2.RCGResourceType;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable compiled graph.
 */

public final class RCGGraph implements RCGGraphType
{
  private final Map<RCGOperationName, RCGOperationType> ops;
  private final Map<RCGResourceName, RCGResourceType> resources;
  private final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph;
  private final Map<RCGPortType, RCGResourceType> portResources;
  private final Map<RCGPortType, RCGOperationImageLayoutTransitionType> imageLayouts;

  /**
   * An immutable compiled graph.
   *
   * @param inGraph         The graph of ports
   * @param inResources     The resources
   * @param inImageLayouts  The image layout transitions
   * @param inOps           The operations
   * @param inPortResources The resources at every port
   */

  public RCGGraph(
    final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> inGraph,
    final Map<RCGOperationName, RCGOperationType> inOps,
    final Map<RCGResourceName, RCGResourceType> inResources,
    final Map<RCGPortType, RCGResourceType> inPortResources,
    final Map<RCGPortType, RCGOperationImageLayoutTransitionType> inImageLayouts)
  {
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.ops =
      Objects.requireNonNull(inOps, "ops");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.portResources =
      Objects.requireNonNull(inPortResources, "portResources");
    this.imageLayouts =
      Objects.requireNonNull(inImageLayouts, "imageLayouts");
  }

  @Override
  public RCGResourceType resourceAt(
    final RCGPortType port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.portResources.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return r;
  }

  @Override
  public RCGOperationImageLayoutTransitionType imageTransitionAt(
    final RCGPortType port)
  {
    Objects.requireNonNull(port, "port");

    final var r = this.imageLayouts.get(port);
    if (r == null) {
      throw new IllegalStateException("Nonexistent port: %s".formatted(port));
    }
    return r;
  }
}
