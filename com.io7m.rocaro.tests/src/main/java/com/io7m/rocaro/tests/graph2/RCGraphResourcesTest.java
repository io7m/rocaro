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
import com.io7m.rocaro.vanilla.RCGraph;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphResourcesTest
{
  @Test
  public void testErrorResourceNameConflict0()
    throws RCGGraphException
  {
    final var b =
      RCGraph.builder();

    b.declareResource(
      "Example0",
      ResExample0.factory(),
      NO_PARAMETERS
    );

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.declareResource(
          "Example0",
          ResExample0.factory(),
          NO_PARAMETERS);
      });

    assertEquals("error-graph-name-duplicate", ex.errorCode());
  }

  @Test
  public void testErrorResourceAlreadyAssigned0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation("Example0", OpEx0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpEx1.factory(), NO_PARAMETERS);
    final var r =
      b.declareResource("Res0", ResExample0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);

    final var ex =
      assertThrows(RCGGraphException.class, () -> {
        b.resourceAssign(op1.port0(), r);
      });

    assertEquals("error-graph-resource-already-assigned", ex.errorCode());
  }

  @Test
  public void testErrorResourceNotAssigned0()
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
    final var op1 =
      b.declareOperation(
        "Example1",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(Set.of(), Set.of())
      );

    b.connect(op0.port(), op1.port());

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    assertEquals("error-graph-port-unassigned-resource", ex.errorCode());
  }

  @Test
  public void testResourceTracked0()
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
    final var op1 =
      b.declareOperation(
        "Example1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(Set.of(), Set.of())
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpConsumer0.factory(),
        new OpConsumer0.Parameters(Set.of(), Set.of())
      );
    final var r =
      b.declareResource("Res0", ResExample0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(r, g.resourceAt(op0.port()));
    assertEquals(r, g.resourceAt(op1.port()));
    assertEquals(r, g.resourceAt(op2.port()));
  }
}
