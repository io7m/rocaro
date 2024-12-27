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

import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;

import java.util.Objects;

/**
 * Track the identities of resources through the graph.
 */

public final class RCGPassResourcesTrack
  implements RCGGraphPassType
{
  /**
   * Track the identities of resources through the graph.
   */

  public RCGPassResourcesTrack()
  {

  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
  {
    final var tracked =
      builder.portResourcesTracked();
    final var producers =
      builder.portResources();

    final var graph =
      builder.graph();

    for (final var port : builder.portsOrdered()) {
      for (final var e : graph.incomingEdgesOf(port)) {
        switch (e.targetPort()) {
          case final RCGPortConsumerType v -> {
            final var r = tracked.get(e.sourcePort());
            Objects.requireNonNull(r, "r");
            tracked.put(v, r);
          }
          case final RCGPortModifierType m -> {
            final var r = tracked.get(e.sourcePort());
            Objects.requireNonNull(r, "r");
            tracked.put(m, r);
          }
        }
      }

      for (final var e : graph.outgoingEdgesOf(port)) {
        switch (e.sourcePort()) {
          case final RCGPortProducerType v -> {
            final RCGResourcePlaceholderType r = producers.get(v);
            Objects.requireNonNull(r, "r");
            tracked.put(v, r);
          }
          case final RCGPortModifierType _ -> {
            // Nothing to do.
          }
        }
      }
    }
  }
}
