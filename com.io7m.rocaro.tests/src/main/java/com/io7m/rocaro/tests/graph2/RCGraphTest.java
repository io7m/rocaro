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
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Post;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Pre;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.PreAndPost;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.tests.graph2.OpImageTransition0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_UNDEFINED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphTest
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
  public void testErrorResourceNotAssigned0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation("Example0", OpProducer0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpConsumer0.factory(), NO_PARAMETERS);

    b.connect(op0.port0(), op1.port0());

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    assertEquals("error-graph-port-unassigned-resource", ex.errorCode());
  }

  @Test
  public void testErrorPortNotConnected0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation("Example0", OpProducer0.factory(), NO_PARAMETERS);

    final var ex =
      assertThrows(RCGGraphException.class, b::compile);

    assertEquals("error-graph-ports-unconnected", ex.errorCode());
  }

  @Test
  public void testResourceTracked0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation("Example0", OpProducer0.factory(), NO_PARAMETERS);
    final var op1 =
      b.declareOperation("Example1", OpModifier0.factory(), NO_PARAMETERS);
    final var op2 =
      b.declareOperation("Example2", OpConsumer0.factory(), NO_PARAMETERS);
    final var r =
      b.declareResource("Res0", ResExample0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(r, g.resourceAt(op0.port0()));
    assertEquals(r, g.resourceAt(op1.port0()));
    assertEquals(r, g.resourceAt(op2.port0()));
  }

  /**
   * No transitions occur.
   */

  @Test
  public void testImageLayoutTransitionUndefinedConstant()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(Optional.empty(), Optional.empty())
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(Optional.empty())
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op2.port0())
    );
  }

  /**
   * A modifier port performs an image layout transition prior to executing.
   */

  @Test
  public void testImageLayoutTransitionPreModifier0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Optional.empty())
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(Optional.empty())
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port0())
    );
  }

  /**
   * A consumer port performs an image layout transition prior to executing.
   */

  @Test
  public void testImageLayoutTransitionPreConsumer0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(Optional.empty(), Optional.empty())
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(Optional.of(
          LAYOUT_OPTIMAL_FOR_ATTACHMENT))
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port0())
    );
  }

  /**
   * A consumer port has a layout requirement that's already met.
   */

  @Test
  public void testImageLayoutTransitionPreConsumer1()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Optional.empty()
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port0())
    );
  }

  /**
   * A modifier port performs both pre and post image layout transitions.
   */

  @Test
  public void testImageLayoutTransitionPrePostModifier0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(Optional.empty())
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new PreAndPost(
        new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
        new Post(
          LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
          LAYOUT_OPTIMAL_FOR_ATTACHMENT)
      ),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port0())
    );
  }

  /**
   * A modifier port performs a post image layout transition.
   */

  @Test
  public void testImageLayoutTransitionPostModifier0()
    throws Exception
  {
    final var b =
      RCGraph.builder();

    final var op0 =
      b.declareOperation(
        "Example0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageTransition0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(Optional.empty())
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port0(), r);
    b.connect(op0.port0(), op1.port0());
    b.connect(op1.port0(), op2.port0());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
      g.imageTransitionAt(op0.port0())
    );
    assertEquals(
      new Post(
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        LAYOUT_OPTIMAL_FOR_ATTACHMENT
      ),
      g.imageTransitionAt(op1.port0())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port0())
    );
  }
}
