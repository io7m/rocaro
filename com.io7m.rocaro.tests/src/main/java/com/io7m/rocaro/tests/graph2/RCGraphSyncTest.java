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

import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.tests.graph2.OpConsumer0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Execute;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.ImageReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.ImageWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.MemoryReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.MemoryWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Read;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Submission;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncCommandType.Write;
import com.io7m.rocaro.vanilla.internal.graph.RCGSyncDependency;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_COPY;
import static com.io7m.rocaro.api.graph.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_SHADER_READ;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static java.lang.Integer.MAX_VALUE;
import static org.jgrapht.nio.AttributeType.DOUBLE;
import static org.jgrapht.nio.AttributeType.STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class RCGraphSyncTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphSyncTest.class);

  @Test
  public void testBarriersPre()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder("Main");

    final var r =
      b.declareResource("R", ResBuffer0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpConsumer0.factory(),
        new Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of()
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg);

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

    /*
     * Operation 0.
     */

    {
      /*
       * There are no dependencies on anything.
       */

      final var dependencies = List.copyOf(sg.outgoingEdgesOf(cmdOp0));
      assertEquals(List.of(), dependencies);

      /*
       * The operation's one and only write operation depends on this operation.
       */

      final var dependents = List.copyOf(sg.incomingEdgesOf(cmdOp0));
      assertEquals(1, dependents.size());
      final var dep = dependents.get(0);
      assertEquals(cmdOp0, dep.before());
      final var op0Write = assertInstanceOf(Write.class, dep.after());
      assertEquals(r, op0Write.resource());
      assertEquals(STAGE_TRANSFER_COPY, op0Write.writesAt());
    }

    /*
     * Operation 1.
     */

    {
      /*
       * We have one read that depends ultimately on the write
       * performed by operation 0.
       */

      final var paths =
        finder.getAllPaths(cmdOp1, cmdOp0, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(cmdOp1, c);
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          MemoryReadBarrier.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op0, c.operation());
            assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(cmdOp0, c);
          }
        )
      );
    }

    /*
     * Operation 2.
     */

    {
      /*
       * We have one read that depends ultimately on the write
       * performed by operation 1.
       */

      final var paths =
        finder.getAllPaths(cmdOp2, cmdOp1, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(cmdOp2, c);
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          MemoryReadBarrier.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(cmdOp1, c);
          }
        )
      );
    }
  }

  @Test
  public void testBarriersImageLayoutPre()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder("Main");

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

    show(sg);

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

    final Write op0Write;
    final Write op1Write;

    /*
     * Operation 0.
     */

    {
      /*
       * There are no dependencies on anything.
       */

      final var dependencies = List.copyOf(sg.outgoingEdgesOf(cmdOp0));
      assertEquals(List.of(), dependencies);

      /*
       * The operation's one and only write operation depends on this operation.
       */

      final var dependents = List.copyOf(sg.incomingEdgesOf(cmdOp0));
      assertEquals(1, dependents.size());
      final var dep = dependents.get(0);
      assertEquals(cmdOp0, dep.before());
      op0Write = assertInstanceOf(Write.class, dep.after());
      assertEquals(r, op0Write.resource());
      assertEquals(STAGE_TRANSFER_COPY, op0Write.writesAt());
    }

    /*
     * Operation 1.
     */

    {
      /*
       * We have one write that depends ultimately on the write
       * performed by operation 0. There are two paths by which it can
       * get there.
       */

      final var dependencies =
        List.copyOf(sg.incomingEdgesOf(cmdOp1));

      assertEquals(1, dependencies.size());

      final var dep = dependencies.get(0);
      assertEquals(cmdOp1, dep.before());

      op1Write = assertInstanceOf(Write.class, dep.after());

      final var paths =
        finder.getAllPaths(op1Write, cmdOp0, true, MAX_VALUE);
      assertEquals(2, paths.size());

      /*
       * The first path reaches the write via the read barrier.
       */

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Write.class,
          e -> {
            assertEquals(op1Write, e);
          }
        ),
        new CommandCheck<>(
          Execute.class,
          e -> {
            assertEquals(op1, e.operation());
          }
        ),
        new CommandCheck<>(
          Read.class,
          e -> {
            assertEquals(op1, e.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, e.readsAt());
          }
        ),
        new CommandCheck<>(
          ImageReadBarrier.class,
          e -> {
            assertEquals(op1, e.operation());
            assertEquals(STAGE_TRANSFER_COPY, e.waitsForWriteAt());
            assertEquals(
              STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
              e.blocksReadAt()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
              e.layoutFrom()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_ATTACHMENT,
              e.layoutTo()
            );
          }
        ),
        new CommandCheck<>(
          Write.class,
          e -> {
            assertEquals(op0, e.operation());
            assertEquals(STAGE_TRANSFER_COPY, e.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          e -> {
            assertEquals(cmdOp0, e);
          }
        )
      );

      /*
       * The second path reaches the write via the write barrier.
       */

      checkPath(
        paths.get(1),
        new CommandCheck<>(
          Write.class,
          e -> {
            assertEquals(op1Write, e);
          }
        ),
        new CommandCheck<>(
          ImageWriteBarrier.class,
          e -> {
            assertEquals(op1, e.operation());
            assertEquals(STAGE_TRANSFER_COPY, e.waitsForWriteAt());
            assertEquals(
              STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
              e.blocksWriteAt()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
              e.layoutFrom()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_ATTACHMENT,
              e.layoutTo()
            );
          }
        ),
        new CommandCheck<>(
          Write.class,
          e -> {
            assertEquals(op0, e.operation());
            assertEquals(STAGE_TRANSFER_COPY, e.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          e -> {
            assertEquals(cmdOp0, e);
          }
        )
      );
    }

    /*
     * Operation 2.
     */

    {
      /*
       * We have one read that depends ultimately on the write
       * performed by operation 1.
       */

      final var paths =
        finder.getAllPaths(cmdOp2, cmdOp1, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          e -> {
            assertEquals(cmdOp2, e);
          }
        ),
        new CommandCheck<>(
          Read.class,
          e -> {
            assertEquals(op2, e.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, e.readsAt());
          }
        ),
        new CommandCheck<>(
          ImageReadBarrier.class,
          e -> {
            assertEquals(op2, e.operation());
            assertEquals(
              STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
              e.blocksReadAt()
            );
            assertEquals(
              STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
              e.waitsForWriteAt()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_ATTACHMENT,
              e.layoutFrom()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_PRESENTATION,
              e.layoutTo()
            );
          }
        ),
        new CommandCheck<>(
          Write.class,
          e -> {
            assertEquals(op1, e.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, e.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          e -> {
            assertEquals(cmdOp1, e);
          }
        )
      );
    }
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
  public void testBarriersImageLayoutPost()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder("Main");

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

    show(sg);

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

    /*
     * Operation 0.
     */

    {
      /*
       * There are no dependencies on anything.
       */

      final var dependencies = List.copyOf(sg.outgoingEdgesOf(cmdOp0));
      assertEquals(List.of(), dependencies);

      /*
       * The operation's one and only write operation depends on this operation.
       */

      final var dependents = List.copyOf(sg.incomingEdgesOf(cmdOp0));
      assertEquals(1, dependents.size());
      final var dep = dependents.get(0);
      assertEquals(cmdOp0, dep.before());
      final var op0Write = assertInstanceOf(Write.class, dep.after());
      assertEquals(r, op0Write.resource());
      assertEquals(STAGE_TRANSFER_COPY, op0Write.writesAt());
    }

    /*
     * Operation 1.
     */

    {
      /*
       * We have one write that depends ultimately on the write
       * performed by operation 0.
       */

      final var dependents =
        List.copyOf(sg.incomingEdgesOf(cmdOp1));

      final var cmd1Write =
        assertInstanceOf(Write.class, dependents.get(0).after());

      final var paths =
        finder.getAllPaths(cmd1Write, cmdOp0, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op1, c.operation());
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          MemoryReadBarrier.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op0, c.operation());
            assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op0, c.operation());
          }
        )
      );
    }

    /*
     * Operation 2.
     */

    {
      /*
       * We have one read that depends ultimately on the write
       * performed by operation 1.
       */

      final var paths =
        finder.getAllPaths(cmdOp2, cmdOp1, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op2, c.operation());
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          ImageReadBarrier.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
            assertEquals(
              LAYOUT_OPTIMAL_FOR_SHADER_READ,
              c.layoutFrom()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_PRESENTATION,
              c.layoutTo()
            );
          }
        ),
        new CommandCheck<>(
          ImageWriteBarrier.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
            assertEquals(
              LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
              c.layoutFrom()
            );
            assertEquals(
              LAYOUT_OPTIMAL_FOR_SHADER_READ,
              c.layoutTo()
            );
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op1, c.operation());
          }
        )
      );
    }
  }

  @Test
  public void testBarriersImageLayoutPrePost()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder("Main");

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
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());
    b.compile();

    final var sg =
      b.syncGraph();

    show(sg);

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

    /*
     * Operation 0.
     */

    {
      /*
       * There are no dependencies on anything.
       */

      final var dependencies = List.copyOf(sg.outgoingEdgesOf(cmdOp0));
      assertEquals(List.of(), dependencies);

      /*
       * The operation's one and only write operation depends on this operation.
       */

      final var dependents = List.copyOf(sg.incomingEdgesOf(cmdOp0));
      assertEquals(1, dependents.size());
      final var dep = dependents.get(0);
      assertEquals(cmdOp0, dep.before());
      final var op0Write = assertInstanceOf(Write.class, dep.after());
      assertEquals(r, op0Write.resource());
      assertEquals(STAGE_TRANSFER_COPY, op0Write.writesAt());
    }

    /*
     * Operation 1.
     */

    {
      /*
       * The operation depends on a write made by operation 0, and must go
       * through a read barrier to see it.
       */

      final var paths =
        finder.getAllPaths(cmdOp1, cmdOp0, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op1, c.operation());
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          ImageReadBarrier.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_TRANSFER_COPY, c.waitsForWriteAt());
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op0, c.operation());
            assertEquals(STAGE_TRANSFER_COPY, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op0, c.operation());
          }
        )
      );
    }

    /*
     * Operation 2.
     */

    {
      /*
       * The operation depends on a write made by operation 1, and must go
       * through a read barrier to see it.
       */

      final var paths =
        finder.getAllPaths(cmdOp2, cmdOp1, true, MAX_VALUE);
      assertEquals(1, paths.size());

      checkPath(
        paths.get(0),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op2, c.operation());
          }
        ),
        new CommandCheck<>(
          Read.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.readsAt());
          }
        ),
        new CommandCheck<>(
          MemoryReadBarrier.class,
          c -> {
            assertEquals(op2, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksReadAt());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
          }
        ),
        new CommandCheck<>(
          ImageWriteBarrier.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.blocksWriteAt());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.waitsForWriteAt());
            assertEquals(LAYOUT_OPTIMAL_FOR_SHADER_READ, c.layoutFrom());
            assertEquals(LAYOUT_OPTIMAL_FOR_PRESENTATION, c.layoutTo());
          }
        ),
        new CommandCheck<>(
          Write.class,
          c -> {
            assertEquals(op1, c.operation());
            assertEquals(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT, c.writesAt());
          }
        ),
        new CommandCheck<>(
          Execute.class,
          c -> {
            assertEquals(op1, c.operation());
          }
        )
      );
    }
  }

  private static void show(
    final DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> g)
  {
    try (final var writer = new StringWriter()) {
      final var exporter =
        new DOTExporter<RCGSyncCommandType, RCGSyncDependency>();

      exporter.setEdgeAttributeProvider(c -> {
        return Map.ofEntries(

        );
      });

      exporter.setGraphAttributeProvider(() -> {
        return Map.ofEntries(
          Map.entry(
            "fontname", new DefaultAttribute<>("Monospaced", STRING)
          ),
          Map.entry(
            "fontsize", new DefaultAttribute<>(9.0, DOUBLE)
          ),
          Map.entry(
            "splines", new DefaultAttribute<>("ortho", STRING)
          ),
          Map.entry(
            "rankdir", new DefaultAttribute<>("TB", STRING)
          ),
          Map.entry(
            "size", new DefaultAttribute<>("12.0", DOUBLE)
          )
        );
      });

      exporter.setVertexAttributeProvider(c -> {
        final String subName =
          "(%s,%s)".formatted(
            c.submission().queue(),
            c.submission().id()
          );

        final String opName =
          switch (c) {
            case final Execute cc -> {
              yield cc.operation().name().value();
            }
            case final Submission cc -> {
              yield "";
            }
            case final ImageReadBarrier cc -> {
              yield cc.operation().name().value();
            }
            case final ImageWriteBarrier cc -> {
              yield cc.operation().name().value();
            }
            case final Read cc -> {
              yield cc.operation().name().value();
            }
            case final MemoryReadBarrier cc -> {
              yield cc.operation().name().value();
            }
            case final Write cc -> {
              yield cc.operation().name().value();
            }
            case final MemoryWriteBarrier cc -> {
              yield cc.operation().name().value();
            }
          };

        final String resName =
          switch (c) {
            case final Execute cc -> {
              yield "";
            }
            case final ImageWriteBarrier cc -> {
              yield cc.resource().name().value();
            }
            case final ImageReadBarrier cc -> {
              yield cc.resource().name().value();
            }
            case final Read cc -> {
              yield cc.resource().name().value();
            }
            case final MemoryReadBarrier cc -> {
              yield cc.resource().name().value();
            }
            case final Write cc -> {
              yield cc.resource().name().value();
            }
            case final MemoryWriteBarrier cc -> {
              yield cc.resource().name().value();
            }
            case final Submission cc -> {
              yield "";
            }
          };

        final String label =
          switch (c) {
            case final Submission cc -> {
              yield "{<f0> Submission %s}".formatted(subName);
            }
            case final Execute cc -> {
              yield "{<f0> [%s] Execute | <f1> Submission %s}".formatted(
                opName,
                subName);
            }
            case final ImageWriteBarrier cc -> {
              yield "{<f0> [%s] ImageWriteBarrier (%s → %s) | <f1> Submission %s | <f2> Resource %s | <f3> WAITS FOR WRITE %s | <f4> BLOCKS WRITE %s}"
                .formatted(
                  opName,
                  cc.layoutFrom(),
                  cc.layoutTo(),
                  subName,
                  resName,
                  cc.waitsForWriteAt(),
                  cc.blocksWriteAt()
                );
            }
            case final ImageReadBarrier cc -> {
              yield "{<f0> [%s] ImageReadBarrier (%s → %s) | <f1> Submission %s | <f2> Resource %s | <f3> WAITS FOR WRITE %s | <f4> BLOCKS READ %s}"
                .formatted(
                  opName,
                  cc.layoutFrom(),
                  cc.layoutTo(),
                  subName,
                  resName,
                  cc.waitsForWriteAt(),
                  cc.blocksReadAt()
                );
            }
            case final Read cc -> {
              yield "{<f0> [%s] Read | <f1> Submission %s | <f2> Resource %s | <f3> %s}"
                .formatted(
                  opName,
                  subName,
                  resName,
                  cc.readsAt()
                );
            }
            case final MemoryReadBarrier cc -> {
              yield "{<f0> [%s] MemoryReadBarrier | <f1> Submission %s | <f2> Resource %s | <f3> WAITS FOR WRITE %s | <f4> BLOCKS READ %s}"
                .formatted(
                  opName,
                  subName,
                  resName,
                  cc.waitsForWriteAt(),
                  cc.blocksReadAt()
                );
            }
            case final Write cc -> {
              yield "{<f0> [%s] Write | <f1> Submission %s | <f2> Resource %s | <f3> %s}"
                .formatted(
                  opName,
                  subName,
                  resName,
                  cc.writesAt()
                );
            }
            case final MemoryWriteBarrier cc -> {
              yield "{<f0> [%s] MemoryWriteBarrier | <f1> Submission %s | <f2> Resource %s | <f3> WAITS FOR WRITE %s | <f4> BLOCKS WRITE %s}"
                .formatted(
                  opName,
                  subName,
                  resName,
                  cc.waitsForWriteAt(),
                  cc.blocksWriteAt()
                );
            }
          };

        return Map.ofEntries(
          Map.entry(
            "label", new DefaultAttribute<>(label, STRING)
          ),
          Map.entry(
            "fontname", new DefaultAttribute<>("Terminus", STRING)
          ),
          Map.entry(
            "fontsize", new DefaultAttribute<>(9.0, DOUBLE)
          ),
          Map.entry(
            "shape", new DefaultAttribute<>("record", STRING)
          ),
          Map.entry(
            "group", new DefaultAttribute<>(opName, STRING)
          )
        );
      });

      exporter.exportGraph(g, writer);
      writer.append('\n');
      writer.flush();

      try {
        Files.writeString(Paths.get("/tmp/graph.dot"), writer.toString());
      } catch (final IOException e) {
        // Don't care.
      }

      System.out.println(writer);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
