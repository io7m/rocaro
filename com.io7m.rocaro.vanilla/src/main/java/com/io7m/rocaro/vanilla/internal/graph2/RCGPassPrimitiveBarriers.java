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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Post;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Pre;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.PreAndPost;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortConsumerType;
import com.io7m.rocaro.api.graph2.RCGPortProducerType;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceImageType;
import com.io7m.rocaro.api.graph2.RCGResourceType;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockReadOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.BlockWriteOnExternal;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionPre;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithImageLayoutTransitionsPrePost;
import com.io7m.rocaro.vanilla.internal.graph2.RCGResourcePrimitiveBarriersType.WithoutImageLayoutTransition;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Produce primitive barriers for each graph operation.
 */

public final class RCGPassPrimitiveBarriers
  implements RCGGraphPassType
{
  /**
   * Produce primitive barriers for each graph operation.
   */

  public RCGPassPrimitiveBarriers()
  {

  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
  {
    final var opGraph =
      builder.opGraph();
    final var iter =
      new TopologicalOrderIterator<>(opGraph);

    while (iter.hasNext()) {
      this.processOp(builder, iter.next());
    }
  }

  private void processOp(
    final RCGGraphBuilderInternalType builder,
    final RCGOperationType op)
  {
    final var barriers =
      builder.operationPrimitiveBarriers();

    Preconditions.checkPreconditionV(
      !barriers.containsKey(op.name()),
      "Operations must only be processed once."
    );

    final var resourceBarriers =
      new HashMap<RCGResourceType, RCGResourcePrimitiveBarriersType>();

    final var portResources =
      builder.portResourcesTracked();

    for (final var port : op.ports()) {
      final var resource = portResources.get(port);
      resourceBarriers.put(resource, this.processPort(builder, port, resource));
    }

    barriers.put(
      op.name(),
      new RCGOperationPrimitiveBarriers(resourceBarriers)
    );
  }

  private RCGResourcePrimitiveBarriersType processPort(
    final RCGGraphBuilderInternalType builder,
    final RCGPortType port,
    final RCGResourceType resource)
  {
    final var portGraph =
      builder.graph();

    final var imageLayoutTransition =
      builder.portImageLayouts().get(port);

    return switch (imageLayoutTransition) {
      case final Constant _ -> {
        final var readBarriersPre =
          new HashSet<BlockReadOnExternal>();
        final var writeBarriersPre =
          new HashSet<BlockWriteOnExternal>();

        collectConsumerReadsWrites(
          port,
          portGraph,
          readBarriersPre,
          writeBarriersPre
        );

        yield new WithoutImageLayoutTransition(
          port.owner(),
          resource,
          readBarriersPre,
          writeBarriersPre
        );
      }

      case final Post post -> {
        Preconditions.checkPreconditionV(
          port instanceof RCGPortProducerType,
          "Port must be a producer."
        );
        Preconditions.checkPreconditionV(
          resource instanceof RCGResourceImageType,
          "Resource must be an image."
        );

        final var readBarriersPre =
          new HashSet<BlockReadOnExternal>();
        final var writeBarriersPre =
          new HashSet<BlockWriteOnExternal>();

        collectConsumerReadsWrites(
          port,
          portGraph,
          readBarriersPre,
          writeBarriersPre
        );

        yield new WithImageLayoutTransitionPost(
          port.owner(),
          (RCGResourceImageType) resource,
          readBarriersPre,
          writeBarriersPre,
          post.layoutFrom(),
          post.layoutTo()
        );
      }

      case final Pre pre -> {
        Preconditions.checkPreconditionV(
          port instanceof RCGPortConsumerType,
          "Port must be a consumer."
        );

        final var connection =
          portGraph.incomingEdgesOf(port)
            .iterator()
            .next();

        final var sourcePort =
          connection.sourcePort();
        final var sourceWrites =
          sourcePort.writesOnStages();
        final var targetReads =
          connection.targetPort().readsOnStages();
        final var targetWrites =
          connection.targetPort().writesOnStages();

        yield new WithImageLayoutTransitionPre(
          port.owner(),
          (RCGResourceImageType) resource,
          sourceWrites,
          sourcePort.owner(),
          pre.layoutFrom(),
          pre.layoutTo(),
          targetReads,
          targetWrites
        );
      }

      case final PreAndPost preAndPost -> {
        Preconditions.checkPreconditionV(
          port instanceof RCGPortConsumerType,
          "Port must be a consumer."
        );
        Preconditions.checkPreconditionV(
          port instanceof RCGPortProducerType,
          "Port must be a producer."
        );
        Preconditions.checkPreconditionV(
          resource instanceof RCGResourceImageType,
          "Resource must be an image."
        );

        final var inConnection =
          portGraph.incomingEdgesOf(port)
            .iterator()
            .next();

        final var sourcePort =
          inConnection.sourcePort();
        final var thisPort =
          inConnection.targetPort();
        final var preLayoutWaitWrites =
          sourcePort.writesOnStages();

        yield new WithImageLayoutTransitionsPrePost(
          port.owner(),
          (RCGResourceImageType) resource,
          preLayoutWaitWrites,
          sourcePort.owner(),
          preAndPost.layoutFrom(),
          preAndPost.layoutDuring(),
          thisPort.readsOnStages(),
          thisPort.writesOnStages(),
          preAndPost.layoutTo()
        );
      }
    };
  }

  private static void collectConsumerReadsWrites(
    final RCGPortType port,
    final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> portGraph,
    final HashSet<BlockReadOnExternal> readBarriersPre,
    final HashSet<BlockWriteOnExternal> writeBarriersPre)
  {
    if (port instanceof final RCGPortConsumerType consumer) {
      final var connection =
        portGraph.incomingEdgesOf(consumer)
          .iterator()
          .next();

      final var sourcePort =
        connection.sourcePort();
      final var sourceWrites =
        sourcePort.writesOnStages();
      final var targetReads =
        connection.targetPort().readsOnStages();
      final var targetWrites =
        connection.targetPort().writesOnStages();

      for (final var sourceWrite : sourceWrites) {
        for (final var targetRead : targetReads) {
          readBarriersPre.add(
            new BlockReadOnExternal(
              targetRead,
              sourceWrite,
              sourcePort.owner()
            )
          );
        }
      }
      for (final var sourceWrite : sourceWrites) {
        for (final var targetWrite : targetWrites) {
          writeBarriersPre.add(
            new BlockWriteOnExternal(
              targetWrite,
              sourceWrite,
              sourcePort.owner()
            )
          );
        }
      }
    }
  }
}
