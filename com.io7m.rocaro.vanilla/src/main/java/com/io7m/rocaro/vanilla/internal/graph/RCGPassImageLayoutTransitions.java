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

import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Post;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Pre;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.PreAndPost;
import com.io7m.rocaro.api.graph.RCGPortConsumes;
import com.io7m.rocaro.api.graph.RCGPortModifies;
import com.io7m.rocaro.api.graph.RCGPortProduces;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGResourceTypes;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;

import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_UNDEFINED;

/**
 * Track the image layout transitions through the graph.
 */

public final class RCGPassImageLayoutTransitions
  implements RCGGraphPassType
{
  /**
   * Track the image layout transitions through the graph.
   */

  public RCGPassImageLayoutTransitions()
  {

  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
  {
    final var resources =
      builder.portResources();
    final var graph =
      builder.graph();
    final var transitions =
      builder.portImageLayouts();

    for (final var port : resources.keySet()) {
      tracePortTransitions(graph, transitions, LAYOUT_UNDEFINED, port);
    }
  }

  private static void tracePortTransitions(
    final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph,
    final HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGResourceImageLayout layoutThen,
    final RCGPortType port)
  {
    if (RCGResourceTypes.isImage(port.type())) {
      switch (port) {
        case final RCGPortProduces p -> {
          final var layoutNow =
            p.ensuresImageLayout().orElseThrow();

          transitions.put(port, new Constant(layoutNow));

          for (final var connection : graph.outgoingEdgesOf(p)) {
            tracePortTransitions(
              graph,
              transitions,
              layoutNow,
              connection.targetPort()
            );
          }
        }

        case final RCGPortConsumes p -> {
          final var requiresOpt =
            p.requiresImageLayout();

          if (requiresOpt.isPresent()) {
            final var requires = requiresOpt.get();
            if (layoutThen != requires) {
              transitions.put(p, new Pre(layoutThen, requires));
            } else {
              transitions.put(p, new Constant(layoutThen));
            }
          } else {
            transitions.put(p, new Constant(layoutThen));
          }
        }

        case final RCGPortModifies p -> {
          var layoutDuring = layoutThen;

          final var requiresOpt = p.requiresImageLayout();
          if (requiresOpt.isPresent()) {
            layoutDuring = requiresOpt.get();
          }

          var layoutLeaving = layoutDuring;
          final var ensuresOpt = p.ensuresImageLayout();
          if (ensuresOpt.isPresent()) {
            layoutLeaving = ensuresOpt.get();
          }

          if (layoutThen != layoutDuring) {
            if (layoutDuring != layoutLeaving) {
              transitions.put(
                port,
                new PreAndPost(layoutThen, layoutDuring, layoutLeaving)
              );
            } else {
              transitions.put(port, new Pre(layoutThen, layoutDuring));
            }
          } else {
            if (layoutDuring != layoutLeaving) {
              transitions.put(port, new Post(layoutDuring, layoutLeaving));
            } else {
              transitions.put(port, new Constant(layoutThen));
            }
          }

          for (final var connection : graph.outgoingEdgesOf(p)) {
            tracePortTransitions(
              graph,
              transitions,
              layoutLeaving,
              connection.targetPort()
            );
          }
        }
      }
    }
  }
}
