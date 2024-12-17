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
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.vanilla.RCGraph;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphBasicTest
{
  @Test
  public void testErrorEmpty()
  {
    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        RCGraph.builder().compile();
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
        RCGraph.builder()
          .connect(op0.port0(), op0.port1());
      });

    assertEquals("error-graph-operation-not-declared", ex.errorCode());
  }

  @Test
  public void testErrorOpNameConflict0()
    throws RCGGraphException
  {
    final var b =
      RCGraph.builder();

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
      RCGraph.builder();

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
      RCGraph.builder();

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
      RCGraph.builder();

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
      RCGraph.builder();

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
      RCGraph.builder();

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpEx1.factory(), NO_PARAMETERS);

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.connect(op0.port0(), op1.port1());
      });

    assertEquals("error-graph-port-type-incompatible", ex.errorCode());
  }

  @Test
  public void testErrorPortResourceAlreadyAssigned0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

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
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(Set.of(), Set.of())
      );

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    assertEquals("error-graph-ports-unconnected", ex.errorCode());
  }

}
