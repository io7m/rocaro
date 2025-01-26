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


package com.io7m.rocaro.tests.rgraphc;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.rocaro.rgraphc.internal.json.RCCJson;
import com.io7m.rocaro.rgraphc.internal.checker.RCCCheckers;
import com.io7m.rocaro.rgraphc.internal.loader.RCCLoaders;
import com.io7m.rocaro.rgraphc.internal.parser.RCCGraphParsers;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCDotExporterPortPrimitive;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveProducer;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPrimitivePortGraph;
import com.io7m.rocaro.rgraphc.internal.primitive_sync.RCCDotExporterSyncPrimitive;
import com.io7m.rocaro.rgraphc.internal.primitive_sync.RCCPrimitiveSyncGraph;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclGraph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCCPrimitivePortGraphTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCCPrimitivePortGraphTest.class);

  private static final ObjectMapper OBJECTS =
    RCCJson.createObjectMapper();

  private RCCGraphParsers parsers;
  private RCCCheckers checkers;

  private static String serialize(
    final RCCPrimitivePortGraph r)
    throws IOException
  {
    final var string = new StringWriter();
    final var pp = new DefaultPrettyPrinter();

    final var writer = OBJECTS.writer(pp);
    writer.writeValue(string, r);
    string.flush();
    final var text = string.toString();
    System.out.println(text);
    return text;
  }

  private void dumpGraph(
    final RCCPrimitivePortGraph sg,
    final TestInfo info)
    throws IOException
  {
    final var stringWriter =
      new StringWriter();

    final var name = new StringBuilder();
    name.append(info.getTestClass().orElseThrow().getSimpleName());
    name.append("_");
    name.append(info.getDisplayName());

    try (final var exporter =
           RCCDotExporterPortPrimitive.open(stringWriter, sg.graph(), name.toString())) {
      exporter.execute();
    }

    Files.writeString(
      Paths.get("/tmp/%s.dot".formatted(name)),
      stringWriter.toString()
    );
  }

  private void doTestExample(
    final String name,
    final TestInfo info,
    final String fileName)
    throws Exception
  {
    final var g =
      this.graph(name);

    final var r =
      this.checkers.createChecker()
        .execute(g);

    final var pg =
      RCCPrimitivePortGraph.create(r);

    this.dumpGraph(pg, info);

    assertEquals(
      pg.graph()
        .vertexSet()
        .stream()
        .filter(p -> p instanceof RCCPortPrimitiveProducer)
        .count(),
      pg.resources().size()
    );

    this.checkPrimitiveText(fileName, pg);
  }

  @BeforeEach
  public void setup()
  {
    this.parsers =
      new RCCGraphParsers();
    this.checkers =
      new RCCCheckers(new RCCLoaders());
  }

  @Test
  public void testExample001(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-001.xml", info, "rgraph-001-primitive.json");
  }

  @Test
  public void testExample002(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-002.xml", info, "rgraph-002-primitive.json");
  }

  @Test
  public void testExample003(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-003.xml", info, "rgraph-003-primitive.json");
  }

  @Test
  public void testExample004(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-004.xml", info, "rgraph-004-primitive.json");
  }

  @Test
  public void testExample005(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-005.xml", info, "rgraph-005-primitive.json");
  }

  @Test
  public void testExample006(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-006.xml", info, "rgraph-006-primitive.json");
  }

  @Test
  public void testExample007(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-007.xml", info, "rgraph-007-primitive.json");
  }

  @Test
  public void testExample008(
    final TestInfo info)
    throws Exception
  {
    this.doTestExample("rgraph-008.xml", info, "rgraph-008-primitive.json");
  }


  private void checkPrimitiveText(
    final String fileName,
    final RCCPrimitivePortGraph r)
    throws Exception
  {
    final var textReceived =
      serialize(r).trim();
    final var textExpected =
      this.textOf(fileName).trim();

    assertEquals(textExpected, textReceived);
  }

  private RCUDeclGraph graph(
    final String name)
    throws Exception
  {
    final var file =
      "/com/io7m/rocaro/tests/%s".formatted(name);

    try (final var stream =
           RCCPrimitivePortGraphTest.class.getResourceAsStream(file)) {
      return this.parsers.parse(
        URI.create("urn:fail"),
        stream
      );
    }
  }

  private String textOf(
    final String name)
    throws Exception
  {
    final var file =
      "/com/io7m/rocaro/tests/%s".formatted(name);

    try (final var stream =
           RCCPrimitivePortGraphTest.class.getResourceAsStream(file)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
  }
}
