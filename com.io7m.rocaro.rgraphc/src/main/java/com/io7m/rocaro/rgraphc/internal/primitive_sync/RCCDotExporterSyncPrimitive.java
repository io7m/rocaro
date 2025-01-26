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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.io7m.jtensors.core.unparameterized.vectors.Vector3D;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors3D;
import org.jgrapht.Graph;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.jgrapht.nio.AttributeType.DOUBLE;
import static org.jgrapht.nio.AttributeType.STRING;

/**
 * A dot exporter for render graphs, showing primitive synchronization.
 */

public final class RCCDotExporterSyncPrimitive
  implements AutoCloseable
{
  private final Writer writer;
  private final DOTExporter<RCCCommandType, RCCSyncDependency> exporter;
  private final Graph<RCCCommandType, RCCSyncDependency> graph;

  private RCCDotExporterSyncPrimitive(
    final Writer inWriter,
    final DOTExporter<RCCCommandType, RCCSyncDependency> inExporter,
    final Graph<RCCCommandType, RCCSyncDependency> inGraph)
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

  public static RCCDotExporterSyncPrimitive open(
    final Writer writer,
    final Graph<RCCCommandType, RCCSyncDependency> graph,
    final String name)
  {
    final var exporter =
      new DOTExporter<RCCCommandType, RCCSyncDependency>();

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
      final Vector3D color =
        nodeColor(c);

      return Map.ofEntries(
        Map.entry(
          "color", new DefaultAttribute<>(formatColor(color), STRING)
        ),
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

    return new RCCDotExporterSyncPrimitive(writer, exporter, graph);
  }

  private static String formatColor(
    final Vector3D color)
  {
    final var r = (int) (255.0 * color.x());
    final var g = (int) (255.0 * color.y());
    final var b = (int) (255.0 * color.z());

    return String.format(
      "#%02x%02x%02x"
        .formatted(r, g, b)
    );
  }

  private static Vector3D nodeColor(
    final RCCCommandType c)
  {
    try {
      final var digest =
        MessageDigest.getInstance("SHA256");
      final var result =
        digest.digest(
          c.operationName()
            .value()
            .getBytes(StandardCharsets.UTF_8)
        );
      final var base =
        Vector3D.of(
          result[0],
          result[1],
          result[2]
        );
      final var normal =
        Vectors3D.normalize(base);
      final var absolute =
        Vectors3D.absolute(normal);

      return Vectors3D.scale(absolute, 0.8);
    } catch (final NoSuchAlgorithmException e) {
      return Vector3D.of(0.0, 0.0, 0.0);
    }
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
    final RCCCommandType c)
  {
    return switch (c) {
      case final RCCExecute _ -> {
        yield "";
      }
      case final RCCImageBarrier cc -> {
        yield cc.resource().name();
      }
      case final RCCAccess cc -> {
        yield cc.resource().name();
      }
      case final RCCMemoryBarrier cc -> {
        yield cc.resource().name();
      }
      case final RCCImageBarrierWithQueueTransfer cc -> {
        yield cc.resource().name();
      }
      case final RCCIntroduceMemory cc -> {
        yield cc.resource().name();
      }
      case final RCCIntroduceImage cc -> {
        yield cc.resource().name();
      }
      case final RCCDiscard cc -> {
        yield cc.resource().name();
      }
      case final RCCMemoryBarrierWithQueueTransfer cc -> {
        yield cc.resource().name();
      }
    };
  }

  private static String submissionName(
    final RCCCommandType c)
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
    final RCCCommandType c)
  {
    final var resName =
      resourceName(c);
    final var subName =
      submissionName(c);
    final String opName =
      c.operation().name().value();

    return switch (c) {
      case final RCCIntroduceImage cc -> {
        yield ("{<f0> [%s 0x%s] Intro Image "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> Layout %s "
               + "| <f4> // %s}").formatted(
          opName,
          Long.toUnsignedString(c.commandId(), 16),
          subName,
          resName,
          cc.imageLayout(),
          c.comment()
        );
      }

      case final RCCIntroduceMemory cc -> {
        yield ("{<f0> [%s 0x%s] Intro Memory "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> // %s}").formatted(
          opName,
          Long.toUnsignedString(c.commandId(), 16),
          subName,
          resName,
          c.comment()
        );
      }

      case final RCCExecute cc -> {
        yield ("{<f0> [%s 0x%s] Execute "
               + "| <f1> Submission %s "
               + "| <f2> // %s}").formatted(
          opName,
          Long.toUnsignedString(c.commandId(), 16),
          subName,
          c.comment()
        );
      }

      case final RCCImageBarrier cc -> {
        yield ("{<f0> [%s 0x%s] ImageBarrier (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> BLOCKS READ %s "
               + "| <f6> // %s} "
        )
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWritesAt(),
            cc.blocksWritesAt(),
            cc.blocksReadsAt(),
            cc.comment()
          );
      }

      case final RCCMemoryBarrier cc -> {
        yield ("{<f0> [%s 0x%s] MemoryBarrier "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> BLOCKS READ %s "
               + "| <f6> // %s} "
        )
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWritesAt(),
            cc.blocksWritesAt(),
            cc.blocksReadsAt(),
            cc.comment()
          );
      }

      case final RCCAccess cc -> {
        yield ("{<f0> [%s 0x%s] Access "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> Read %s"
               + "| <f4> Write %s "
               + "| <f5> // %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.readsAt(),
            cc.writesAt(),
            cc.comment()
          );
      }

      case final RCCMemoryBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] MemoryReadBarrierWithQueueTransfer "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> BLOCKS READ %s "
               + "| <f6> Queue %s → %s "
               + "| <f7> // %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.waitsForWritesAt(),
            cc.blocksWritesAt(),
            cc.blocksReadsAt(),
            cc.queueSource(),
            cc.queueTarget(),
            cc.comment()
          );
      }

      case final RCCImageBarrierWithQueueTransfer cc -> {
        yield ("{<f0> [%s 0x%s] ImageBarrierWithQueueTransfer (%s → %s) "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> WAITS FOR WRITE %s "
               + "| <f4> BLOCKS WRITE %s "
               + "| <f5> BLOCKS READ %s "
               + "| <f6> Queue %s → %s "
               + "| <f7> // %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            cc.layoutFrom(),
            cc.layoutTo(),
            subName,
            resName,
            cc.waitsForWritesAt(),
            cc.blocksWritesAt(),
            cc.blocksReadsAt(),
            cc.queueSource(),
            cc.queueTarget(),
            cc.comment()
          );
      }

      case final RCCDiscard cc -> {
        yield ("{<f0> [%s 0x%s] Discard "
               + "| <f1> Submission %s "
               + "| <f2> Resource %s "
               + "| <f3> // %s}")
          .formatted(
            opName,
            Long.toUnsignedString(c.commandId(), 16),
            subName,
            resName,
            cc.comment()
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
