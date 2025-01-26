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


package com.io7m.rocaro.rgraphc.internal.primitive_graph;

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
 * A dot exporter for render graphs, showing primitive ports.
 */

public final class RCCDotExporterPortPrimitive
  implements AutoCloseable
{
  private final Writer writer;
  private final DOTExporter<RCCPortPrimitiveType, RCCPortPrimitiveConnection> exporter;
  private final Graph<RCCPortPrimitiveType, RCCPortPrimitiveConnection> graph;

  private RCCDotExporterPortPrimitive(
    final Writer inWriter,
    final DOTExporter<RCCPortPrimitiveType, RCCPortPrimitiveConnection> inExporter,
    final Graph<RCCPortPrimitiveType, RCCPortPrimitiveConnection> inGraph)
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

  public static RCCDotExporterPortPrimitive open(
    final Writer writer,
    final Graph<RCCPortPrimitiveType, RCCPortPrimitiveConnection> graph,
    final String name)
  {
    final var exporter =
      new DOTExporter<RCCPortPrimitiveType, RCCPortPrimitiveConnection>();

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
      return fixName(c.fullPath().toString());
    });

    exporter.setVertexAttributeProvider(c -> {
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
          "group", new DefaultAttribute<>(c.owner().name().value(), STRING)
        )
      );
    });

    return new RCCDotExporterPortPrimitive(writer, exporter, graph);
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

  private static String nodeLabel(
    final RCCPortPrimitiveType c)
  {
    return switch (c) {
      case final RCCPortPrimitiveProducer pp -> {
        yield "{<f0> %s | <f1> Operation %s | <f2> %s | <f3> READS %s | <f4> WRITES %s}".formatted(
          "Producer",
          pp.owner().name().value(),
          pp.fullPath(),
          pp.reads(),
          pp.writes()
        );
      }
      case final RCCPortPrimitiveConsumer pp -> {
        yield "{<f0> %s | <f1> Operation %s | <f2> %s | <f3> READS %s | <f4> WRITES %s}".formatted(
          "Consumer",
          pp.owner().name().value(),
          pp.fullPath(),
          pp.reads(),
          pp.writes()
        );
      }
      case final RCCPortPrimitiveModifier pp -> {
        yield "{<f0> %s | <f1> Operation %s | <f2> %s | <f3> READS %s | <f4> WRITES %s}".formatted(
          "Modifier",
          pp.owner().name().value(),
          pp.fullPath(),
          pp.reads(),
          pp.writes()
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
