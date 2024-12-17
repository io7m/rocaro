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


package com.io7m.rocaro.tests.graph2;

import com.io7m.rocaro.api.graph2.RCGGraphException;
import com.io7m.rocaro.api.graph2.RCGResourceImageLayout;
import com.io7m.rocaro.tests.graph2.OpConsumer0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph2.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph2.RCGGraphExplain;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockReadOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockWriteOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPre;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionsPrePost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithoutImageLayoutTransition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph2.RCGCommandPipelineStage.STAGE_TRANSFER_COPY;
import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_SHADER_READ;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RCGraphExplainTest
{
  private static void explain(
    final RCGGraphBuilderInternalType graph)
    throws RCGGraphException
  {
    graph.compile();

    final var out = new ByteArrayOutputStream();
    try (final var writer = new PrintWriter(out)) {
      final var explain = new RCGGraphExplain(graph, writer);
      graph.traverse(explain);
      writer.flush();
    }

    System.out.println(out);
  }

  @Test
  public void testImageBarriersPre()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResImage0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.empty()
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.empty()
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    explain(b);
  }

  @Test
  public void testImageBarriersPrePost()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResImage0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.empty()
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    explain(b);
  }
}
