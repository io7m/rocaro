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

import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Post;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Pre;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.PreAndPost;
import com.io7m.rocaro.tests.graph2.OpImageModifier0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_UNDEFINED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCGraphImageLayoutTransitionsTest
{
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.empty(),
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op2.port())
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port())
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.empty(),
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port())
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new Pre(LAYOUT_UNDEFINED, LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port())
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_UNDEFINED)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_UNDEFINED),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new PreAndPost(
        LAYOUT_UNDEFINED,
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        LAYOUT_OPTIMAL_FOR_ATTACHMENT
      ),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port())
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
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        )
      );
    final var op1 =
      b.declareOperation(
        "Example1",
        OpImageModifier0.factory(),
        new Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
          Set.of(),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT)
        )
      );
    final var op2 =
      b.declareOperation(
        "Example2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(),
          Set.of(),
          Optional.empty()
        )
      );
    final var r =
      b.declareResource("Image0", ResImage0.factory(), NO_PARAMETERS);

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
      g.imageTransitionAt(op0.port())
    );
    assertEquals(
      new Post(
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        LAYOUT_OPTIMAL_FOR_ATTACHMENT
      ),
      g.imageTransitionAt(op1.port())
    );
    assertEquals(
      new Constant(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
      g.imageTransitionAt(op2.port())
    );
  }
}
