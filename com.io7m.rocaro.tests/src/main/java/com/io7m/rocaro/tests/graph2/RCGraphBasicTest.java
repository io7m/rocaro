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
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.vanilla.RCGraph;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.graph.RCGNoParameters.NO_PARAMETERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphBasicTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphBasicTest.class);

  @Test
  public void testErrorEmpty()
  {
    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        RCGraph.builder("Main").compile();
      });

    assertEquals("error-graph-empty", ex.errorCode());
  }

  @Test
  public void testErrorOpNotDeclared0()
  {
    final var op0 =
      new OpEx0(new RCGOperationName("Example0"));

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        RCGraph.builder("Main")
          .connect(op0.port0(), op0.port1());
      });

    assertEquals("error-graph-operation-not-declared", ex.errorCode());
  }

  @Test
  public void testErrorOpNameConflict0()
    throws RCGGraphException
  {
    final var b =
      RCGraph.builder("Main");

    b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
      });

    assertEquals("error-graph-name-duplicate", ex.errorCode());
  }

  @Test
  public void testErrorOpNotDeclared1()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);

    final var op1 =
      new OpEx0(new RCGOperationName("Example1"));

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.connect(op0.port0(), op1.port1());
      });

    assertEquals("error-graph-operation-not-declared", ex.errorCode());
  }

  @Test
  public void testErrorOpNotDeclared2()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op1 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);

    final var op0 =
      new OpEx0(new RCGOperationName("Example1"));

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.connect(op0.port0(), op1.port1());
      });

    assertEquals("error-graph-operation-not-declared", ex.errorCode());
  }

  @Test
  public void testErrorPortAlreadyConnected0()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpEx0.factory(), NO_PARAMETERS);

    b.connect(op0.port0(), op1.port1());

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.connect(op0.port0(), op1.port1());
      });

    assertEquals("error-graph-port-already-connected", ex.errorCode());
  }

  @Test
  public void testErrorPortAlreadyConnected1()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpEx0.factory(), NO_PARAMETERS);
    final var op2 =
      b.declareOperation("Example2", OpEx0.factory(), NO_PARAMETERS);

    b.connect(op0.port0(), op1.port1());

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.connect(op2.port0(), op1.port1());
      });

    assertEquals("error-graph-port-already-connected", ex.errorCode());
  }

  @Test
  public void testErrorPortMistyped0()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var r =
      b.declareResource("R", ResBuffer0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Example0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(GRAPHICS, Set.of(), Set.of())
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          GRAPHICS,
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );

    b.connect(op0.port(), op1.port());
    b.resourceAssign(op0.port(), r);

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    show(ex);
    assertEquals("error-graph-type-incompatible", ex.errorCode());
  }

  private static void show(
    final RCGGraphException ex)
  {
    LOG.debug("Message: {}", ex.getMessage());
    LOG.debug("Error Code: {}", ex.errorCode());
    for (final var entry : ex.attributes().entrySet()) {
      LOG.debug("Attribute: {}: {}", entry.getKey(), entry.getValue());
    }
    LOG.debug("Remediating: {}", ex.remediatingAction());
  }

  @Test
  public void testErrorPortResourceAlreadyAssigned0()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
    final var r =
      b.declareResource("Res0", ResExample0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.resourceAssign(op0.port0(), r);
      });

    assertEquals("error-graph-port-already-assigned", ex.errorCode());
  }

  @Test
  public void testErrorPortNotConnected0()
    throws Exception
  {
    final var b =
      RCGraph.builder("Main");

    final var op0 =
      b.declareOperation(
        "Example0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(GRAPHICS, Set.of(), Set.of())
      );

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    assertEquals("error-graph-ports-unconnected", ex.errorCode());
  }

}
