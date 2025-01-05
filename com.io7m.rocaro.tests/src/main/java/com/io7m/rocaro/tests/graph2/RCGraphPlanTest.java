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

import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.tests.graph2.OpConsumer0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSRead;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSWrite;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.COMPUTE;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.TRANSFER;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_COMPUTE_SHADER;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_COPY;
import static java.lang.Integer.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class RCGraphPlanTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphPlanTest.class);

  private static final RCResourceSchematicBufferType ANY_BUFFER =
    () -> 100L;

  private static final RCResourceSchematicImage2DType ANY_IMAGE =
    new RCResourceSchematicImage2DType()
    {
      @Override
      public Vector2I size()
      {
        return Vector2I.of(128, 128);
      }

      @Override
      public VulkanFormat format()
      {
        return VulkanFormat.VK_FORMAT_R8_UNORM;
      }

      @Override
      public boolean isPresentationImage()
      {
        return false;
      }
    };

  private static final RCStrings STRINGS =
    new RCStrings(Locale.getDefault());

  @Test
  public void testBarriersPre(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_BUFFER);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(
          GRAPHICS,
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpConsumer0.factory(),
        new Parameters(
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of()
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    final var g = b.compile();

    final var p = g.executionPlan();
    assertEquals(1, p.submissions().size());

    final var s = p.submissions().get(0);
    assertEquals(new RCGSubmissionID(GRAPHICS, 0), s.submissionId());
    assertEquals(1, s.items().size());
  }

  @Test
  public void testBarriersComputeTransfer(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_BUFFER);

    final var stages =
      Set.of(STAGE_COMPUTE_SHADER);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(TRANSFER, stages, stages)
      );

    final var op1 =
      b.declareOperation(
        "Op1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(TRANSFER, stages, stages)
      );

    final var op2 =
      b.declareOperation(
        "Op2",
        OpModifier0.factory(),
        new OpModifier0.Parameters(COMPUTE, stages, stages)
      );

    final var op3 =
      b.declareOperation(
        "Op3",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(COMPUTE, stages, stages)
      );

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.connect(op2.port(), op3.port());
    final var g = b.compile();

    final var p = g.executionPlan();
    final var s = p.submissions();
    assertEquals(2, s.size());
  }
}
