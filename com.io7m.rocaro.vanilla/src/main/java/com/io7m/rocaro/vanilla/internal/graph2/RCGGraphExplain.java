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

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGResourceType;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPre;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionsPrePost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithoutImageLayoutTransition;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A human-readable description of barrier operations.
 */

public final class RCGGraphExplain
  implements Consumer<RCGOperationType>
{
  private final PrintWriter writer;
  private final RCGGraphBuilderInternalType graph;

  /**
   * Construct an explainer.
   *
   * @param inGraph  The graph builder
   * @param inWriter The output writer
   */

  public RCGGraphExplain(
    final RCGGraphBuilderInternalType inGraph,
    final PrintWriter inWriter)
  {
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
  }

  @Override
  public void accept(
    final RCGOperationType operation)
  {
    final var opBarriers =
      this.graph.operationPrimitiveBarriers()
        .get(operation.name());

    final var resources = new HashSet<RCGResourceType>();
    for (final var port : operation.ports()) {
      resources.add(
        this.graph.portResourcesTracked()
          .get(port)
      );
    }

    this.writer.println(operation);

    for (final var resource : resources) {
      final var resBarriers =
        opBarriers.barriers().get(resource);

      switch (resBarriers) {
        case final WithImageLayoutTransitionPost b -> {
          throw new UnimplementedCodeException();
        }

        case final WithImageLayoutTransitionPre b -> {
          for (final var w : b.layoutWaitsOnWrites()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Layout barrier: Pause my layout transition [");
            this.writer.print(b.preLayoutFrom());
            this.writer.print(" → ");
            this.writer.print(b.preLayoutTo());
            this.writer.print("] until WRITE on ");
            this.writer.print(w);
            this.writer.print(" by ");
            this.writer.print(b.operationPerformingWrite());
            this.writer.println();
          }

          this.writer.print("  - ");
          this.writer.print(resource);
          this.writer.print(": Layout transition [");
          this.writer.print(b.preLayoutFrom());
          this.writer.print(" → ");
          this.writer.print(b.preLayoutTo());
          this.writer.println("]");

          for (final var r : b.readsWaitOnLayout()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Read barrier: Pause my READ on ");
            this.writer.print(r);
            this.writer.print(" until my layout transition");
            this.writer.println();
          }
          for (final var w : b.writesWaitOnLayout()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Write barrier: Pause my WRITE on ");
            this.writer.print(w);
            this.writer.print(" until my layout transition");
            this.writer.println();
          }
        }

        case final WithImageLayoutTransitionsPrePost b -> {
          for (final var w : b.layoutWaitsOnWrites()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Layout barrier: Pause my layout transition [");
            this.writer.print(b.preLayoutFrom());
            this.writer.print(" → ");
            this.writer.print(b.preLayoutTo());
            this.writer.print("] until WRITE on ");
            this.writer.print(w);
            this.writer.print(" by ");
            this.writer.print(b.operationPerformingWrite());
            this.writer.println();
          }

          this.writer.print("  - ");
          this.writer.print(resource);
          this.writer.print(": Layout transition [");
          this.writer.print(b.preLayoutFrom());
          this.writer.print(" → ");
          this.writer.print(b.preLayoutTo());
          this.writer.println("]");

          for (final var r : b.readsWaitOnPreLayout()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Read barrier: Pause my READ on ");
            this.writer.print(r);
            this.writer.print(" until my layout transition");
            this.writer.println();
          }
          for (final var w : b.writesWaitOnPreLayout()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Write barrier: Pause my WRITE on ");
            this.writer.print(w);
            this.writer.print(" until my layout transition");
            this.writer.println();
          }
        }

        case final WithoutImageLayoutTransition b -> {
          for (final var r : b.readBarriersPre()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Read barrier: Pause my READ on ");
            this.writer.print(r.blocksReadOn());
            this.writer.print(" until WRITE on ");
            this.writer.print(r.waitsOnWrite());
            this.writer.print(" by ");
            this.writer.print(r.operationPerformingWrite());
            this.writer.println();
          }
          for (final var r : b.writeBarriersPre()) {
            this.writer.print("  - ");
            this.writer.print(resource);
            this.writer.print(": ");
            this.writer.print("Write barrier: Pause my WRITE on ");
            this.writer.print(r.blocksWriteOn());
            this.writer.print(" until WRITE on ");
            this.writer.print(r.waitsOnWrite());
            this.writer.print(" by ");
            this.writer.print(r.operationPerformingWrite());
            this.writer.println();
          }
        }
      }
    }

    this.writer.println("  - Execute");

    for (final var resource : resources) {
      final var resBarriers =
        opBarriers.barriers().get(resource);

      switch (resBarriers) {
        case final WithImageLayoutTransitionPost b -> {
          this.writer.print("  - ");
          this.writer.print(resource);
          this.writer.print(": ");
          this.writer.print("Layout transition [");
          this.writer.print(b.postLayoutFrom());
          this.writer.print(" → ");
          this.writer.print(b.postLayoutTo());
          this.writer.print("]");
          this.writer.println();
        }

        case final WithImageLayoutTransitionPre b -> {
          // Nothing to do
        }

        case final WithImageLayoutTransitionsPrePost b -> {
          this.writer.print("  - ");
          this.writer.print(resource);
          this.writer.print(": Layout transition [");
          this.writer.print(b.preLayoutTo());
          this.writer.print(" → ");
          this.writer.print(b.postLayoutTo());
          this.writer.println("]");
        }

        case final WithoutImageLayoutTransition _ -> {
          // Nothing to do
        }
      }
    }

    this.writer.println();
  }
}
