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


package com.io7m.rocaro.vanilla.internal.graph.layout_transitions;

import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Post;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Pre;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.PreAndPost;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.resources.RCSchematicConstraintBuffer;
import com.io7m.rocaro.api.resources.RCSchematicConstraintImage2D;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphPassType;
import com.io7m.rocaro.vanilla.internal.graph.RCGPassAbstract;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGGraphPrimitiveConnection;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPassPortPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveConsumer;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveModifier;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveProducer;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveType;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_UNDEFINED;

/**
 * Track the image layout transitions through the graph.
 */

public final class RCGPassImageLayoutTransitions
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  private DirectedAcyclicGraph<RCGPortPrimitiveType, RCGGraphPrimitiveConnection> portGraph;

  /**
   * Track the image layout transitions through the graph.
   */

  public RCGPassImageLayoutTransitions()
  {
    super(Set.of(RCGPassPortPrimitive.class));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
  {
    final var resources =
      builder.portResources();
    this.portGraph =
      builder.primitivePortGraph();
    final var portToPrimitives =
      builder.portToPrimitives();
    final var transitions =
      builder.portImageLayouts();

    for (final var port : resources.keySet()) {
      final var primitives =
        portToPrimitives.get(port);

      for (final var primitive : primitives.values()) {
        this.tracePortTransitions(transitions, LAYOUT_UNDEFINED, primitive);
      }
    }
  }

  private void tracePortTransitions(
    final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGResourceImageLayout layoutThen,
    final RCGPortPrimitiveType port)
  {
    switch (port) {
      case final RCGPortPrimitiveModifier p -> {
        this.tracePortTransitionsModifier(transitions, layoutThen, p);
      }
      case final RCGPortPrimitiveProducer p -> {
        this.tracePortTransitionsProducer(transitions, p);
      }
      case final RCGPortPrimitiveConsumer p -> {
        tracePortTransitionsConsumer(transitions, layoutThen, p);
      }
    }
  }

  private void tracePortTransitionsModifier(
    final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGResourceImageLayout layoutThen,
    final RCGPortPrimitiveModifier port)
  {
    final Optional<RCGResourceImageLayout> requiresOpt;
    final Optional<RCGResourceImageLayout> ensuresOpt;

    switch (port.typeConsumed()) {
      case final RCSchematicConstraintBuffer<?, ?> _ -> {
        return;
      }
      case final RCSchematicConstraintImage2D<?, ?> i -> {
        requiresOpt = i.requiresImageLayout();
      }
    }

    switch (port.typeProduced()) {
      case final RCSchematicConstraintBuffer<?, ?> _ -> {
        return;
      }
      case final RCSchematicConstraintImage2D<?, ?> i -> {
        ensuresOpt = i.requiresImageLayout();
      }
    }

    this.tracePortTransitionsModifierActual(
      transitions,
      layoutThen,
      port,
      requiresOpt,
      ensuresOpt
    );
  }

  private void tracePortTransitionsModifierActual(
    final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGResourceImageLayout layoutThen,
    final RCGPortPrimitiveModifier port,
    final Optional<RCGResourceImageLayout> requiresOpt,
    final Optional<RCGResourceImageLayout> ensuresOpt)
  {
    var layoutDuring = layoutThen;
    if (requiresOpt.isPresent()) {
      layoutDuring = requiresOpt.get();
    }

    var layoutLeaving = layoutDuring;
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

    for (final var connection : this.portGraph.outgoingEdgesOf(port)) {
      this.tracePortTransitions(
        transitions,
        layoutLeaving,
        connection.targetPort()
      );
    }
  }

  private void tracePortTransitionsProducer(
    final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGPortPrimitiveProducer port)
  {
    final RCGResourceImageLayout layoutNow;
    switch (port.typeProduced()) {
      case final RCSchematicConstraintBuffer<?, ?> _ -> {
        return;
      }
      case final RCSchematicConstraintImage2D<?, ?> i -> {
        layoutNow = i.requiresImageLayout().orElseThrow();
      }
    }

    transitions.put(port, new Constant(layoutNow));

    for (final var connection : this.portGraph.outgoingEdgesOf(port)) {
      this.tracePortTransitions(
        transitions,
        layoutNow,
        connection.targetPort()
      );
    }
  }

  private static void tracePortTransitionsConsumer(
    final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> transitions,
    final RCGResourceImageLayout layoutThen,
    final RCGPortPrimitiveConsumer port)
  {
    final Optional<RCGResourceImageLayout> requiresOpt;

    switch (port.typeConsumed()) {
      case final RCSchematicConstraintBuffer<?, ?> _ -> {
        return;
      }
      case final RCSchematicConstraintImage2D<?, ?> i -> {
        requiresOpt = i.requiresImageLayout();
      }
    }

    if (requiresOpt.isPresent()) {
      final var requires = requiresOpt.get();
      if (layoutThen != requires) {
        transitions.put(port, new Pre(layoutThen, requires));
      } else {
        transitions.put(port, new Constant(layoutThen));
      }
    } else {
      transitions.put(port, new Constant(layoutThen));
    }
  }
}
