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


package com.io7m.rocaro.vanilla.internal.graph.port_primitive;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGResourceSubname;
import com.io7m.rocaro.api.resources.RCSchematicConstraintCompositeType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintPrimitiveType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphPassType;
import com.io7m.rocaro.vanilla.internal.graph.RCGPassAbstract;
import com.io7m.rocaro.vanilla.internal.graph.RCGPassCheckNonEmpty;
import com.io7m.rocaro.vanilla.internal.graph.RCGPassCheckPortsConnected;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * A pass that builds the primitive port graph from the high-level port graph.
 */

public final class RCGPassPortPrimitive
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  private HashMap<RCGPortType<?>, HashMap<RCGResourceSubname, RCGPortPrimitiveType>> portToPrimitives;
  private DirectedAcyclicGraph<RCGPortPrimitiveType, RCGGraphPrimitiveConnection> primitiveGraph;

  /**
   * A pass that builds the primitive port graph from the high-level port graph.
   */

  public RCGPassPortPrimitive()
  {
    super(Set.of(
      RCGPassCheckNonEmpty.class,
      RCGPassCheckPortsConnected.class
    ));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    this.portToPrimitives =
      builder.portToPrimitives();
    final var portGraph =
      builder.graph();
    this.primitiveGraph =
      builder.primitivePortGraph();

    for (final var port : portGraph.vertexSet()) {
      switch (port) {
        case final RCGPortConsumerType<?> p ->
          this.processCreateConsumerPorts(p);
        case final RCGPortModifierType<?> p ->
          this.processCreateModifierPorts(p);
        case final RCGPortProducerType<?> p ->
          this.processCreateProducerPorts(p);
      }
    }

    for (final var edge : portGraph.edgeSet()) {
      this.processCreateEdge(edge);
    }
  }

  private void processCreateEdge(
    final RCGGraphConnection edge)
  {
    final var sourcePort =
      edge.sourcePort();
    final var targetPort =
      edge.targetPort();

    final var sourcePrimitivePorts =
      this.portToPrimitives.get(sourcePort);
    final var targetPrimitivePorts =
      this.portToPrimitives.get(targetPort);

    Invariants.checkInvariantV(
      sourcePrimitivePorts.size(),
      sourcePrimitivePorts.size() == targetPrimitivePorts.size(),
      "Primitive port counts must match."
    );

    for (final var sourceEntry : sourcePrimitivePorts.entrySet()) {
      final var targetPrimitive =
        targetPrimitivePorts.get(sourceEntry.getKey());

      Objects.requireNonNull(targetPrimitive, "targetPrimitive");

      this.primitiveGraph.addEdge(
        sourceEntry.getValue(),
        targetPrimitive,
        new RCGGraphPrimitiveConnection(
          sourceEntry.getValue(),
          targetPrimitive
        )
      );
    }
  }

  private void processCreateModifierPorts(
    final RCGPortModifierType<?> p)
  {
    switch (p.typeConsumed()) {
      case final RCSchematicConstraintCompositeType<?> ccc -> {
        switch (p.typeProduced()) {
          case final RCSchematicConstraintPrimitiveType<?> _ -> {
            throw new UnreachableCodeException();
          }

          case final RCSchematicConstraintCompositeType<?> pcc -> {
            final var consumerConstraints =
              ccc.primitiveConstraints();
            final var producerConstraints =
              pcc.primitiveConstraints();

            for (final var entry : consumerConstraints.entrySet()) {
              final var consumerConstraint =
                entry.getValue();
              final var producerConstraint =
                producerConstraints.get(entry.getKey());

              this.addPort(
                p,
                new RCGPortPrimitiveModifier(
                  p,
                  entry.getKey(),
                  consumerConstraint,
                  producerConstraint
                )
              );
            }
          }
        }
      }

      case final RCSchematicConstraintPrimitiveType<?> ccp -> {
        switch (p.typeProduced()) {
          case final RCSchematicConstraintPrimitiveType<?> pcp -> {
            this.addPort(
              p,
              new RCGPortPrimitiveModifier(
                p,
                new RCGResourceSubname("Main"),
                ccp,
                pcp
              )
            );
          }

          case final RCSchematicConstraintCompositeType<?> _ -> {
            throw new UnreachableCodeException();
          }
        }
      }
    }
  }

  private void processCreateProducerPorts(
    final RCGPortProducerType<?> p)
  {
    switch (p.typeProduced()) {
      case final RCSchematicConstraintCompositeType<?> cc -> {
        final var entries =
          cc.primitiveConstraints().entrySet();

        for (final var entry : entries) {
          this.addPort(
            p,
            new RCGPortPrimitiveProducer(
              p,
              entry.getKey(),
              entry.getValue()
            )
          );
        }
      }
      case final RCSchematicConstraintPrimitiveType<?> cp -> {
        this.addPort(
          p,
          new RCGPortPrimitiveProducer(
            p,
            new RCGResourceSubname("Main"),
            cp
          )
        );
      }
    }
  }

  private void processCreateConsumerPorts(
    final RCGPortConsumerType<?> p)
  {
    switch (p.typeConsumed()) {
      case final RCSchematicConstraintCompositeType<?> cc -> {
        final var entries =
          cc.primitiveConstraints().entrySet();

        for (final var entry : entries) {
          this.addPort(
            p,
            new RCGPortPrimitiveConsumer(
              p,
              entry.getKey(),
              entry.getValue()
            )
          );
        }
      }
      case final RCSchematicConstraintPrimitiveType<?> cp -> {
        this.addPort(
          p,
          new RCGPortPrimitiveConsumer(
            p,
            new RCGResourceSubname("Main"),
            cp
          )
        );
      }
    }
  }

  private void addPort(
    final RCGPortType<?> p,
    final RCGPortPrimitiveType pp)
  {
    final var existing =
      this.portToPrimitives.computeIfAbsent(p, _ -> new HashMap<>());

    Preconditions.checkPreconditionV(
      !existing.containsKey(pp.subName()),
      "Port subnames must be unique."
    );

    existing.put(pp.subName(), pp);
    this.primitiveGraph.addVertex(pp);
  }
}
