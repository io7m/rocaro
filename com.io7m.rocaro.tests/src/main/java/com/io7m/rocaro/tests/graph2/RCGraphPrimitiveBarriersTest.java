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
import com.io7m.rocaro.tests.graph2.OpConsumer0.Parameters;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph2.RCGGraphBuilderInternalType;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockReadOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockWriteOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPre;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionsPrePost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithoutImageLayoutTransition;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph2.RCGCommandPipelineStage.STAGE_TRANSFER_COPY;
import static com.io7m.rocaro.api.graph2.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_SHADER_READ;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RCGraphPrimitiveBarriersTest
{
  @Test
  public void testBarriersPre()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResBuffer0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpProducer0.factory(),
        new OpProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpModifier0.factory(),
        new OpModifier0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpConsumer0.factory(),
        new Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of()
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();

    /*
     * The first operation doesn't need to read anything, and doesn't
     * need to wait in order to write.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op0.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(Set.of(), resBarriers.readBarriersPre());
      assertEquals(Set.of(), resBarriers.writeBarriersPre());
    }

    /*
     * The second operation:
     *
     * - Needs a read barrier at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT in order
     *   to wait on STAGE_TRANSFER_COPY from op0
     * - Needs a write barrier at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT in order
     *   to wait on STAGE_TRANSFER_COPY from op0
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op1.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(
        Set.of(
          new BlockReadOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_TRANSFER_COPY,
            op0
          )
        ),
        resBarriers.readBarriersPre()
      );
      assertEquals(
        Set.of(
          new BlockWriteOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_TRANSFER_COPY,
            op0
          )
        ),
        resBarriers.writeBarriersPre()
      );
    }

    /*
     * The third operation:
     *
     * - Needs a read barrier at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT in order
     *   to wait on STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT from op1
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op2.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(
        Set.of(
          new BlockReadOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            op1
          )
        ),
        resBarriers.readBarriersPre()
      );

      assertEquals(Set.of(), resBarriers.writeBarriersPre());
    }
  }

  @Test
  public void testBarriersImageLayoutPre()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResImage0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.empty()
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();

    /*
     * The first operation doesn't need to read anything, and doesn't
     * need to wait in order to write.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op0.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(Set.of(), resBarriers.readBarriersPre());
      assertEquals(Set.of(), resBarriers.writeBarriersPre());
    }

    /*
     * The second operation:
     *
     * - The layout transition must wait on a write at STAGE_TRANSFER_COPY
     *   by the external operation.
     * - The read at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT must wait on the
     *   layout transition.
     * - The write at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT must wait on the
     *   layout transition.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op1.name());
      final var resBarriers =
        (WithImageLayoutTransitionPre) opBarriers.barriers().get(r);

      assertEquals(
        op0,
        resBarriers.operationPerformingWrite()
      );
      assertEquals(
        Set.of(STAGE_TRANSFER_COPY),
        resBarriers.layoutWaitsOnWrites()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        resBarriers.preLayoutFrom()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_ATTACHMENT,
        resBarriers.preLayoutTo()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.readsWaitOnLayout()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.writesWaitOnLayout()
      );
    }

    /*
     * The third operation:
     *
     * - The layout transition must wait on a write at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT
     *   by the external operation.
     * - The read at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT must wait on the
     *   layout transition.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op2.name());
      final var resBarriers =
        (WithImageLayoutTransitionPre) opBarriers.barriers().get(r);

      assertEquals(
        op1,
        resBarriers.operationPerformingWrite()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.layoutWaitsOnWrites()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_ATTACHMENT,
        resBarriers.preLayoutFrom()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_PRESENTATION,
        resBarriers.preLayoutTo()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.readsWaitOnLayout()
      );
      assertEquals(
        Set.of(),
        resBarriers.writesWaitOnLayout()
      );
    }
  }

  @Test
  public void testBarriersImageLayoutPost()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResImage0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          Optional.empty(),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();

    /*
     * The first operation doesn't need to read anything, and doesn't
     * need to wait in order to write.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op0.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(Set.of(), resBarriers.readBarriersPre());
      assertEquals(Set.of(), resBarriers.writeBarriersPre());
    }

    /*
     * The second operation:
     *
     * - Needs a read barrier at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT in order
     *   to wait on STAGE_TRANSFER_COPY from op0
     * - Needs a write barrier at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT in order
     *   to wait on STAGE_TRANSFER_COPY from op0
     * - Needs a layout transition that waits on the write at
     *   STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT from this operation
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op1.name());
      final var resBarriers =
        (WithImageLayoutTransitionPost) opBarriers.barriers().get(r);

      assertEquals(
        Set.of(
          new BlockReadOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_TRANSFER_COPY,
            op0
          )
        ),
        resBarriers.readBarriersPre()
      );
      assertEquals(
        Set.of(
          new BlockWriteOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_TRANSFER_COPY,
            op0
          )
        ),
        resBarriers.writeBarriersPre()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        resBarriers.postLayoutFrom()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_SHADER_READ,
        resBarriers.postLayoutTo()
      );
    }

    /*
     * The third operation:
     *
     * - The layout transition must wait on a write at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT
     *   by the external operation.
     * - The read at STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT must wait on the
     *   layout transition.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op2.name());
      final var resBarriers =
        (WithImageLayoutTransitionPre) opBarriers.barriers().get(r);

      assertEquals(
        op1,
        resBarriers.operationPerformingWrite()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.layoutWaitsOnWrites()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_SHADER_READ,
        resBarriers.preLayoutFrom()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_PRESENTATION,
        resBarriers.preLayoutTo()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.readsWaitOnLayout()
      );
      assertEquals(
        Set.of(),
        resBarriers.writesWaitOnLayout()
      );
    }
  }

  @Test
  public void testBarriersImageLayoutPrePost()
    throws RCGGraphException
  {
    final var b =
      (RCGGraphBuilderInternalType) RCGraph.builder();

    final var r =
      b.declareResource("R", ResImage0.factory(), NO_PARAMETERS);

    final var op0 =
      b.declareOperation(
        "Op0",
        OpImageProducer0.factory(),
        new OpImageProducer0.Parameters(
          Set.of(),
          Set.of(STAGE_TRANSFER_COPY),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET)
        ));

    final var op1 =
      b.declareOperation(
        "Op1",
        OpImageModifier0.factory(),
        new OpImageModifier0.Parameters(
          Optional.of(LAYOUT_OPTIMAL_FOR_SHADER_READ),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    final var op2 =
      b.declareOperation(
        "Op2",
        OpImageConsumer0.factory(),
        new OpImageConsumer0.Parameters(
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        ));

    b.resourceAssign(op0.port(), r);
    b.connect(op0.port(), op1.port());
    b.connect(op1.port(), op2.port());

    final var g = b.compile();

    /*
     * The first operation doesn't need to read anything, and doesn't
     * need to wait in order to write.
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op0.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(Set.of(), resBarriers.readBarriersPre());
      assertEquals(Set.of(), resBarriers.writeBarriersPre());
    }

    /*
     * The second operation:
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op1.name());
      final var resBarriers =
        (WithImageLayoutTransitionsPrePost) opBarriers.barriers().get(r);

      assertEquals(
        Set.of(STAGE_TRANSFER_COPY),
        resBarriers.layoutWaitsOnWrites()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.readsWaitOnPreLayout()
      );
      assertEquals(
        Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
        resBarriers.writesWaitOnPreLayout()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,
        resBarriers.preLayoutFrom()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_SHADER_READ,
        resBarriers.preLayoutTo()
      );
      assertEquals(
        LAYOUT_OPTIMAL_FOR_PRESENTATION,
        resBarriers.postLayoutTo()
      );
    }

    /*
     * The third operation:
     */

    {
      final var opBarriers =
        b.operationPrimitiveBarriers().get(op2.name());
      final var resBarriers =
        (WithoutImageLayoutTransition) opBarriers.barriers().get(r);

      assertEquals(
        Set.of(
          new BlockReadOnExternal(
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,
            op1
          )
        ),
        resBarriers.readBarriersPre()
      );
      assertEquals(
        Set.of(),
        resBarriers.writeBarriersPre()
      );
    }
  }
}
