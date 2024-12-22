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

import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSImageReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSImageReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSImageWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSImageWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSMemoryReadBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSMemoryReadBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSMemoryWriteBarrier;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSMemoryWriteBarrierWithQueueTransfer;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSRead;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSWrite;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSyncDependency;
import org.jgrapht.Graph;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.jgrapht.nio.AttributeType.DOUBLE;
import static org.jgrapht.nio.AttributeType.STRING;

/**
 * A dot exporter for render graphs.
 */

public final class RCGDotExporter
  implements AutoCloseable
{
  private final Writer writer;
  private final DOTExporter<RCGSyncCommandType, RCGSyncDependency> exporter;
  private final Graph<RCGSyncCommandType, RCGSyncDependency> graph;

  private RCGDotExporter(
    final Writer inWriter,
    final DOTExporter<RCGSyncCommandType, RCGSyncDependency> inExporter,
    final Graph<RCGSyncCommandType, RCGSyncDependency> inGraph)
  {
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
    this.exporter =
      Objects.requireNonNull(inExporter, "exporter");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
  }

  private static final Pattern BAD_CHARS =
    Pattern.compile("[^a-zA-Z_0-9]+");

  /**
   * Open a dot exporter.
   *
   * @param writer The writer
   * @param graph  The graph
   * @param name   The graph name
   *
   * @return The exporter
   */

  public static RCGDotExporter open(
    final Writer writer,
    final Graph<RCGSyncCommandType, RCGSyncDependency> graph,
    final String name)
  {
    final var exporter =
      new DOTExporter<RCGSyncCommandType, RCGSyncDependency>();

    exporter.setEdgeAttributeProvider(c -> {
      return Map.ofEntries(

      );
    });

    exporter.setGraphAttributeProvider(() -> {
      return Map.ofEntries(
        Map.entry(
          "label", new DefaultAttribute<>(fixName(name), STRING)
        ),
        Map.entry(
          "fontname", new DefaultAttribute<>("Terminus", STRING)
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

    exporter.setVertexIdProvider(c -> {
      return "\"0x%s\"".formatted(Long.toUnsignedString(c.commandId(), 16));
    });

    exporter.setVertexAttributeProvider(c -> {
      final String opName =
        c.operation().name().value();
      final String label =
        nodeLabel(c);

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

    return new RCGDotExporter(writer, exporter, graph);
  }

  private static String fixName(
    final String name)
  {
    return name.replaceAll(BAD_CHARS.pattern(), "_");
  }

  /**
   * Execute the exporter.
   *
   * @throws IOException On errors
   */

  public void execute()
    throws IOException
  {
    this.exporter.exportGraph(this.graph, this.writer);
    this.writer.flush();
  }

  private static String resourceName(
    final RCGSyncCommandType c)
  {
    return switch (c) {
      case final RCGSExecute _ -> {
        yield "";
      }
      case final RCGSImageWriteBarrier cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSImageReadBarrier cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSRead cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSMemoryReadBarrier cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSWrite cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSMemoryWriteBarrier cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSImageReadBarrierWithQueueTransfer cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSMemoryReadBarrierWithQueueTransfer cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSImageWriteBarrierWithQueueTransfer cc -> {
        yield cc.resource().name().value();
      }
      case final RCGSMemoryWriteBarrierWithQueueTransfer cc -> {
        yield cc.resource().name().value();
      }
    };
  }

  private static String submissionName(
    final RCGSyncCommandType c)
  {
    if (c.submission() != null) {
      return "(%s,%s)".formatted(
        c.submission().queue(),
        c.submission().submissionId()
      );
    } else {
      return "(_,_)";
    }
  }

  private static String nodeLabel(
    final RCGSyncCommandType c)
  {
    final var resName =
      resourceName(c);
    final var subName =
      submissionName(c);
    final String opName =
      c.operation().name().value();

    return switch (c) {
      case final RCGSExecute cc -> {
        yield ("{<f0> [%s 0x%s] Execute "
               + "| <f1> Submission %s}").formatted(
          opName,
          Long.toUnsignedString(c.commandId(), 16),
          subName);
      }

      case final RCGSImageWriteBarrier cc -> {
        yield ("{<f0> [%s 0x%s] ImageWriteBarrier (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksWriteAt()
          );
      }

      case final RCGSImageReadBarrier cc -> {
        yield ("{<f0> [%s 0x%s] ImageReadBarrier (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS READ %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksReadAt()
          );
      }

      case final RCGSRead cc -> {
        yield ("{<f0> [%s 0x%s] Read "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.readsAt()
          );
      }

      case final RCGSMemoryReadBarrier cc -> {
        yield ("{<f0> [%s 0x%s] MemoryReadBarrier "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS READ %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksReadAt()
          );
      }

      case final RCGSWrite cc -> {
        yield ("{<f0> [%s 0x%s] Write "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.writesAt()
          );
      }

      case final RCGSMemoryWriteBarrier cc -> {
        yield ("{<f0> [%s 0x%s] MemoryWriteBarrier "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksWriteAt()
          );
      }

      case final RCGSImageReadBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] ImageReadBarrierWithQueueTransfer (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS READ %s "
               + "| <f5> Queue %s → %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksReadAt(),
            cc.queueSource(),
            cc.queueTarget()
          );
      }

      case final RCGSMemoryReadBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] MemoryReadBarrierWithQueueTransfer "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS READ %s "
               + "| <f5> Queue %s → %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksReadAt(),
            cc.queueSource(),
            cc.queueTarget()
          );
      }

      case final RCGSImageWriteBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] ImageWriteBarrierWithQueueTransfer (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> Queue %s → %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksWriteAt(),
            cc.queueSource(),
            cc.queueTarget()
          );
      }

      case final RCGSMemoryWriteBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] MemoryWriteBarrierWithQueueTransfer "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> Queue %s → %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWriteAt(),
            cc.blocksWriteAt(),
            cc.queueSource(),
            cc.queueTarget()
          );
      }
    };
  }

  @Override
  public void close()
    throws IOException
  {
    this.writer.close();
  }
}
