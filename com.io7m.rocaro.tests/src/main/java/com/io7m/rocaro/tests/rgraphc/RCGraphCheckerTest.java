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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.io7m.anethum.api.ParsingException;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.checker.RCCCheckers;
import com.io7m.rocaro.rgraphc.internal.loader.RCCLoaders;
import com.io7m.rocaro.rgraphc.internal.parser.RCCGraphParserParameters;
import com.io7m.rocaro.rgraphc.internal.parser.RCCGraphParsers;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphCheckerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphCheckerTest.class);

  private static final ObjectMapper OBJECTS =
    createObjectMapper();
  private RCCGraphParsers parsers;
  private RCCCheckers checkers;

  private static ObjectMapper createObjectMapper()
  {
    final var module = new SimpleModule();
    module.addSerializer(new RCLexicalPositionSerializer());
    final var mapper = new ObjectMapper();
    mapper.registerModule(module);

    final var jdk8 = new Jdk8Module();
    mapper.registerModule(jdk8);
    return mapper;
  }

  private static String serialize(
    final RCTGraphDeclarationType r)
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

  @BeforeEach
  public void setup()
  {
    this.parsers =
      new RCCGraphParsers();
    this.checkers =
      new RCCCheckers(new RCCLoaders());
  }

  @Test
  public void testExample001()
    throws Exception
  {
    final var g =
      this.graph("rgraph-001.xml");

    final var r =
      this.checkers.createChecker()
        .execute(g);

    this.checkText("rgraph-001.json", r);
  }

  @Test
  public void testExample002()
    throws Exception
  {
    final var g =
      this.graph("rgraph-002.xml");

    final var r =
      this.checkers.createChecker()
        .execute(g);

    this.checkText("rgraph-002.json", r);
  }

  private void checkText(
    final String fileName,
    final RCTGraphDeclarationType r)
    throws Exception
  {
    final var textReceived =
      serialize(r).trim();
    final var textExpected =
      this.textOf(fileName).trim();

    assertEquals(textExpected, textReceived);
  }

  @Test
  public void testErrorCyclic()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-cyclic.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-declaration-circular", ex.errorCode());
  }

  @Test
  public void testErrorFieldDuplicate()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-record-field-duplicate.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-field-name-used", ex.errorCode());
  }

  @Test
  public void testErrorFieldTypeMissing()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-record-field-typemissing.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-type-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorFieldTypeWrong()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-record-field-typeoperation.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-type-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorBadTypeReference0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-bad-type-reference-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-subresource-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortDuplicate0()
    throws Exception
  {
    assertThrows(ParsingException.class, () -> {
      this.graph("rgraph-error-port-duplicate-0.xml");
    });
  }

  @Test
  public void testErrorPortType0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-type-incompatible", ex.errorCode());
  }

  @Test
  public void testErrorPortType1()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-declaration-not-type", ex.errorCode());
  }

  @Test
  public void testErrorPortType2()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-2.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-subresource-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortType3()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-3.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-not-primitive-resource", ex.errorCode());
  }

  @Test
  public void testErrorPortType4()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-4.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-subresource-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortType5()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-type-5.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-declaration-not-type", ex.errorCode());
  }

  @Test
  public void testErrorPortCyclic0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-cyclic-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-cyclic", ex.errorCode());
  }

  @Test
  public void testErrorPortCyclic1()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-cyclic-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-cyclic", ex.errorCode());
  }

  @Test
  public void testErrorPortConnected0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-connected-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-already-connected", ex.errorCode());
  }

  @Test
  public void testErrorPortConnected1()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-connected-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-consumer-one-connection", ex.errorCode());
  }

  @Test
  public void testErrorPortConnected2()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-connected-2.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-producer-one-connection", ex.errorCode());
  }

  @Test
  public void testErrorPortNotSource0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-not-source-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-not-source", ex.errorCode());
  }

  @Test
  public void testErrorPortNotTarget0()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-port-not-target-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-not-target", ex.errorCode());
  }

  @Test
  public void testErrorPortOpNonexistent0()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-op-nonexistent-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-operation-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortOpNonexistent1()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-op-nonexistent-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-operation-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortPortNonexistent0()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-port-nonexistent-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortPortNonexistent1()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-port-nonexistent-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-nonexistent", ex.errorCode());
  }

  @Test
  public void testErrorPortNotConnected0()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-not-connected-0.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-producer-one-connection", ex.errorCode());
  }

  @Test
  public void testErrorPortNotConnected1()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-not-connected-1.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-consumer-one-connection", ex.errorCode());
  }

  @Test
  public void testErrorPortNotConnected2()
    throws Exception
  {
    final var g =
      this.graphWithoutXSD("rgraph-error-port-not-connected-2.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-port-modifier-connections", ex.errorCode());
  }

  @Test
  public void testErrorNotImported()
    throws Exception
  {
    final var g =
      this.graph("rgraph-error-not-imported.xml");

    final var ex =
      assertThrows(RCCompilerException.class, () -> {
        this.checkers.createChecker()
          .execute(g);
      });

    assertEquals("error-package-not-imported", ex.errorCode());
  }

  private RCUDeclGraph graph(
    final String name)
    throws Exception
  {
    final var file =
      "/com/io7m/rocaro/tests/%s".formatted(name);

    try (final var stream =
           RCGraphCheckerTest.class.getResourceAsStream(file)) {
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
           RCGraphCheckerTest.class.getResourceAsStream(file)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
    }
  }

  private RCUDeclGraph graphWithoutXSD(
    final String name)
    throws Exception
  {
    final var file =
      "/com/io7m/rocaro/tests/%s".formatted(name);

    try (final var stream =
           RCGraphCheckerTest.class.getResourceAsStream(file)) {

      final var parser =
        this.parsers.createParserWithContext(
          new RCCGraphParserParameters(
            new JXEHardenedSAXParsers(),
            true
          ),
          URI.create("urn:fail"),
          stream,
          status -> LOG.debug("{}", status)
        );

      return parser.execute();
    }
  }
}
