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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.resources.RCResourceSchematicCompositeType;
import com.io7m.rocaro.api.resources.RCResourceSchematicPrimitiveType;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPassPortPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveConsumer;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveModifier;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveProducer;

import java.util.Objects;
import java.util.Set;

/**
 * Track the identities of primitive resources through the graph.
 */

public final class RCGPassPrimitiveResourcesTrack
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  /**
   * Track the identities of primitive resources through the graph.
   */

  public RCGPassPrimitiveResourcesTrack()
  {
    super(Set.of(
      RCGPassPortPrimitive.class,
      RCGPassPrimitiveTopological.class,
      RCGPassCheckPortResourcesAssigned.class
    ));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
  {
    final var primitivePortResources =
      builder.portPrimitiveResourcesTracked();
    final var graph =
      builder.primitivePortGraph();
    final var ordered =
      builder.primitivePortsOrdered();
    final var portResources =
      builder.portResources();

    for (final var port : ordered) {
      for (final var e : graph.incomingEdgesOf(port)) {
        switch (e.targetPort()) {
          case final RCGPortPrimitiveConsumer c -> {
            final var r =
              primitivePortResources.get(e.sourcePort());
            Objects.requireNonNull(r, "r");
            primitivePortResources.put(c, r);
          }

          case final RCGPortPrimitiveModifier m -> {
            final var r =
              primitivePortResources.get(e.sourcePort());
            Objects.requireNonNull(r, "r");
            primitivePortResources.put(m, r);
          }

          case final RCGPortPrimitiveProducer _ -> {
            throw new UnreachableCodeException();
          }
        }
      }

      for (final var e : graph.outgoingEdgesOf(port)) {
        switch (e.sourcePort()) {
          case final RCGPortPrimitiveConsumer _,
               final RCGPortPrimitiveModifier _ -> {
            // Nothing to do.
          }

          case final RCGPortPrimitiveProducer v -> {
            final var subName =
              v.subName();
            final var resource =
              portResources.get(v.originalPort());

            switch (resource.schematic()) {
              case final RCResourceSchematicCompositeType c -> {
                final var schematics =
                  c.schematics();

                Preconditions.checkPreconditionV(
                  schematics,
                  schematics.containsKey(subName),
                  "Schematics must contain one named '%s'",
                  subName.value()
                );

                final var schematic =
                  schematics.get(subName);

                final var variable =
                  new RCGResourceVariable<>(
                    resource.name().resolveSubName(subName),
                    schematic
                  );

                primitivePortResources.put(v, variable);
              }

              case final RCResourceSchematicPrimitiveType p -> {
                final var variable =
                  new RCGResourceVariable<>(resource.name(), p);

                primitivePortResources.put(v, variable);
              }
            }
          }
        }
      }
    }
  }
}
