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
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.tests.graph2.OpConsumer0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.graph.RCGDotExporterSyncPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSImageWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSMemoryWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSRead;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSWrite;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncDependency;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.COMPUTE;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.TRANSFER;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_COMPUTE_SHADER;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_CPU;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_CLEAR;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_COPY;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_RESOLVE;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_SHADER_READ;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static java.lang.Integer.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class RCGraphSyncTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphSyncTest.class);

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
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);
    final var cmdOp2 = oc.get(op2);
    assertNotNull(cmdOp2);

    final var paths =
      finder.getAllPaths(cmdOp0, cmdOp2, false, MAX_VALUE);

    assertEquals(2, paths.size());

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.operation(), op0);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.blocksWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.operation(), op0);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );
  }

  @Test
  public void testBarriersImageLayoutPre(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_IMAGE);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          GRAPHICS,
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          GRAPHICS,
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.empty()
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);
    final var cmdOp2 = oc.get(op2);
    assertNotNull(cmdOp2);

    final var paths =
      finder.getAllPaths(cmdOp0, cmdOp2, false, MAX_VALUE);

    assertEquals(2, paths.size());

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.operation(), op0);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.blocksWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_ATTACHMENT, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_ATTACHMENT, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writesAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.operation(), op0);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_TRANSFER_COPY);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_ATTACHMENT, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(c.writeStage(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op1);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(c.waitsForWriteAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.blocksReadAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_ATTACHMENT, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(c.readsAt(), STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT);
          assertEquals(c.operation(), op2);
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );
  }

  private record CommandCheck<C extends RCGSyncCommandType>(
    Class<C> clazz,
    Consumer<C> check)
  {
    private CommandCheck
    {
      Objects.requireNonNull(clazz, "clazz");
      Objects.requireNonNull(check, "check");
    }
  }

  private static void checkPath(
    final GraphPath<RCGSyncCommandType, RCGSyncDependency> path,
    final CommandCheck<?>... checks)
  {
    final var vertices = path.getVertexList();
    for (int index = 0; index < vertices.size(); ++index) {
      final var vertex = vertices.get(index);
      LOG.debug("[{}]: {}", index, vertex);
    }

    final var upperBound =
      Math.min(checks.length, vertices.size());

    for (int index = 0; index < upperBound; ++index) {
      final var vertex =
        vertices.get(index);
      final var check =
        checks[index];

      LOG.trace("Check [{}] {}", index, vertex);
      assertEquals(check.clazz, vertex.getClass());
      if (vertex.getClass() == check.clazz) {
        final Consumer<Object> f = (Consumer<Object>) check.check;
        f.accept(vertex);
      }
    }

    assertEquals(
      checks.length,
      vertices.size(),
      "Path length %d must be equal to checks length %d"
        .formatted(checks.length, vertices.size())
    );
  }

  @Test
  public void testBarriersImageLayoutPost(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_IMAGE);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          GRAPHICS,
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          GRAPHICS,
          Optional.empty(),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);
    final var cmdOp2 = oc.get(op2);
    assertNotNull(cmdOp2);

    final var paths =
      finder.getAllPaths(cmdOp0, cmdOp2, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          assertEquals(op0, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op2, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op2, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )

    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          assertEquals(op0, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op2, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op2, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );
  }

  @Test
  public void testBarriersImageLayoutPrePost(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_IMAGE);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          GRAPHICS,
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          GRAPHICS,
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
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);
    final var cmdOp2 = oc.get(op2);
    assertNotNull(cmdOp2);

    final var paths =
      finder.getAllPaths(cmdOp0, cmdOp2, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          assertEquals(op0, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op2, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op2, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )

    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          assertEquals(op0, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrier.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          assertEquals(op2, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op2, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp2, c);
        }
      )
    );
  }

  @Test
  public void testBarriersComputeGraphicsTransfer(
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
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(
          GRAPHICS,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);

    final var op1Write =
      List.copyOf(sg.outgoingEdgesOf(cmdOp1)).getFirst().target();

    final var paths =
      finder.getAllPaths(cmdOp0, op1Write, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op0, c.operation());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(COMPUTE, c.queueSource());
          assertEquals(GRAPHICS, c.queueTarget());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op0, c.operation());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(COMPUTE, c.queueSource());
          assertEquals(GRAPHICS, c.queueTarget());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          assertEquals(op1, c.operation());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );
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

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(
          TRANSFER,
          Set.of(STAGE_TRANSFER_COPY),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);

    final var cmd1Write =
      sg.outgoingEdgesOf(cmdOp1).iterator().next().target();

    final var paths =
      finder.getAllPaths(cmdOp0, cmd1Write, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op0, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(COMPUTE, c.queueSource());
          assertEquals(TRANSFER, c.queueTarget());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op0, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryReadBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_TRANSFER_COPY, c.blocksReadAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(COMPUTE, c.queueSource());
          assertEquals(TRANSFER, c.queueTarget());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_TRANSFER_COPY, c.readsAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );
  }

  @Test
  public void testBarriersComputeModify(
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
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpModifier0.factory(),
        new OpModifier0.Parameters(
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    final var op3 =
      b.declareOperation(
        "Op3",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(
          COMPUTE,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.connect(op2.port(), op3.port());
    b.compile();

    final var sg =
      b.syncGraph();
    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    show(sg, testInfo.getDisplayName());

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);
    final var cmdOp2 = oc.get(op2);
    assertNotNull(cmdOp2);
    final var cmdOp3 = oc.get(op3);
    assertNotNull(cmdOp3);

    final var op3Write =
      List.copyOf(sg.outgoingEdgesOf(cmdOp3)).getFirst().target();

    final var paths =
      finder.getAllPaths(cmdOp0, op3Write, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op0, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrier.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(op1, c.operation());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op1, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrier.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(op2, c.operation());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op2, c.operation());
        }
      ),
      new CommandCheck<>(
        RCGSMemoryWriteBarrier.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(op3, c.operation());
          assertEquals(COMPUTE, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(op3, c.operation());
        }
      )
    );
  }

  @Test
  public void testBarriersImageQueueTransfer0(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_IMAGE);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          TRANSFER,
          Set.of(),
          Set.of(STAGE_COMPUTE_SHADER),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          GRAPHICS,
          Set.of(STAGE_TRANSFER_COPY),
          Set.of(STAGE_COMPUTE_SHADER),
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg, testInfo.getDisplayName());

    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);

    final var cmd1Write =
      sg.outgoingEdgesOf(cmdOp1).iterator().next().target();

    final var paths =
      finder.getAllPaths(cmdOp0, cmd1Write, false, MAX_VALUE);

    checkPath(
      paths.get(0),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op0, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageWriteBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.blocksWriteAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(TRANSFER, c.queueSource());
          assertEquals(GRAPHICS, c.queueTarget());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );

    checkPath(
      paths.get(1),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp0, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op0, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(TRANSFER, c.submission().queue());
          assertEquals(0, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSImageReadBarrierWithQueueTransfer.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_TRANSFER_COPY, c.blocksReadAt());
          assertEquals(STAGE_COMPUTE_SHADER, c.waitsForWriteAt());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
          assertEquals(TRANSFER, c.queueSource());
          assertEquals(GRAPHICS, c.queueTarget());
          assertEquals(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET, c.layoutFrom());
          assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutTo());
        }
      ),
      new CommandCheck<>(
        RCGSRead.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_TRANSFER_COPY, c.readsAt());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      ),
      new CommandCheck<>(
        RCGSExecute.class,
        c -> {
          assertEquals(cmdOp1, c);
        }
      ),
      new CommandCheck<>(
        RCGSWrite.class,
        c -> {
          assertEquals(op1, c.operation());
          assertEquals(STAGE_COMPUTE_SHADER, c.writesAt());
          assertEquals(GRAPHICS, c.submission().queue());
          assertEquals(1, c.submission().submissionId());
        }
      )
    );
  }

  @Test
  public void testBarriersSeparateReadsWrites(
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
          COMPUTE,
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_RESOLVE,
            STAGE_TRANSFER_CLEAR,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_CPU
          ),
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_RESOLVE,
            STAGE_TRANSFER_CLEAR,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_CPU
          )
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(
          COMPUTE,
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_RESOLVE,
            STAGE_TRANSFER_CLEAR,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_CPU
          ),
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_RESOLVE,
            STAGE_TRANSFER_CLEAR,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_CPU
          )
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.compile();

    final var sg =
      b.syncGraph();
    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    show(sg, testInfo.getDisplayName());

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);

    final var op1Write =
      List.copyOf(sg.outgoingEdgesOf(cmdOp1)).getFirst().target();

    final var paths =
      finder.getAllPaths(cmdOp0, op1Write, false, MAX_VALUE);

    assertEquals(30, paths.size());
  }

  @Test
  public void testBarriersSyncMany(
    final TestInfo testInfo)
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder(STRINGS, "Main");

    final var r =
      b.declareResource("R", ANY_BUFFER);
    final var s =
      b.declareResource("S", ANY_BUFFER);
    final var t =
      b.declareResource("T", ANY_BUFFER);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer3.factory(),
        new OpProducer3.Parameters(
          COMPUTE,
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_RESOLVE
          ),
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_CLEAR
          )
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpConsumer3.factory(),
        new OpConsumer3.Parameters(
          COMPUTE,
          Set.of(
            STAGE_TRANSFER_COPY,
            STAGE_TRANSFER_CLEAR
          ),
          Set.of(
            STAGE_TRANSFER_COPY
          )
        ));

    b.resourceAssign(op0.port0(), r);
    b.resourceAssign(op0.port1(), s);
    b.resourceAssign(op0.port2(), t);

    b.connect(op0.port0(), op1.port0());
    b.connect(op0.port1(), op1.port1());
    b.connect(op0.port2(), op1.port2());

    b.compile();

    final var sg =
      b.syncGraph();
    final var oc =
      b.syncOpCommands();
    final var finder =
      new AllDirectedPaths<>(sg);

    show(sg, testInfo.getDisplayName());

    final var cmdOp0 = oc.get(op0);
    assertNotNull(cmdOp0);
    final var cmdOp1 = oc.get(op1);
    assertNotNull(cmdOp1);

    final var op1Write =
      List.copyOf(sg.outgoingEdgesOf(cmdOp1)).getFirst().target();

    final var paths =
      finder.getAllPaths(cmdOp0, op1Write, false, MAX_VALUE);

    assertEquals(38, paths.size());
  }

  private static void show(
    final DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> g,
    final String name)
  {
    try (final var writer = new StringWriter()) {
      try (final var exporter = RCGDotExporterSyncPrimitive.open(writer, g, name)) {
        exporter.execute();
      }
      writer.append('\n');
      writer.flush();

      try {
        Files.writeString(
          Paths.get("/tmp/%s.dot".formatted(name)),
          writer.toString());
      } catch (final IOException e) {
        // Don't care.
      }

      System.out.println(writer);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
