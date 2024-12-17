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

import com.io7m.rocaro.api.graph2.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph2.RCGResourceImageType;
import com.io7m.rocaro.api.graph2.RCGResourceType;

import java.util.Objects;
import java.util.Set;

/**
 * The primitive barriers that surround accesses to a resource in an
 * operation.
 */

public sealed interface RCGResourcePrimitiveBarriersType
{
  /**
   * @return The operation
   */

  RCGOperationType operation();

  /**
   * @return The resource
   */

  RCGResourceType resource();

  /**
   * A barrier that blocks a read on a resource by this operation until
   * a write command has completed from a different operation.
   *
   * @param blocksReadOn             The reads that are blocked by this barrier
   * @param waitsOnWrite             The writes upon which the barrier must wait
   * @param operationPerformingWrite The external operation
   */

  record BlockReadOnExternal(
    RCGCommandPipelineStage blocksReadOn,
    RCGCommandPipelineStage waitsOnWrite,
    RCGOperationType operationPerformingWrite)
  {
    /**
     * A barrier that blocks a read on a resource by this operation until
     * a write command has completed from a different operation.
     */

    public BlockReadOnExternal
    {
      Objects.requireNonNull(blocksReadOn, "read");
      Objects.requireNonNull(waitsOnWrite, "waitsOnWrite");
      Objects.requireNonNull(
        operationPerformingWrite,
        "operationPerformingWrite"
      );
    }
  }

  /**
   * A barrier that blocks a write command on a resource by this operation
   * until a write command has completed from a different operation.
   *
   * @param blocksWriteOn            The writes that are blocked by this barrier
   * @param waitsOnWrite             The writes upon which the barrier must wait
   * @param operationPerformingWrite The external operation
   */

  record BlockWriteOnExternal(
    RCGCommandPipelineStage blocksWriteOn,
    RCGCommandPipelineStage waitsOnWrite,
    RCGOperationType operationPerformingWrite)
  {
    /**
     * A barrier that blocks a write command on a resource by this operation
     * until a write command has completed from a different operation.
     */

    public BlockWriteOnExternal
    {
      Objects.requireNonNull(blocksWriteOn, "write");
      Objects.requireNonNull(waitsOnWrite, "waitsOnWrite");
      Objects.requireNonNull(
        operationPerformingWrite,
        "operationPerformingWrite"
      );
    }
  }

  /**
   * The operation carried out by this resource:
   *
   * <ol>
   *   <li>Waits on the given read barriers.</li>
   *   <li>Waits on the given write barriers.</li>
   *   <li>Executes.</li>
   * </ol>
   *
   * @param operation        The operation
   * @param resource         The resource
   * @param readBarriersPre  The read barriers
   * @param writeBarriersPre The write barriers
   */

  record WithoutImageLayoutTransition(
    RCGOperationType operation,
    RCGResourceType resource,
    Set<BlockReadOnExternal> readBarriersPre,
    Set<BlockWriteOnExternal> writeBarriersPre)
    implements RCGResourcePrimitiveBarriersType
  {
    /**
     * No image layout transition.
     */

    public WithoutImageLayoutTransition
    {
      Objects.requireNonNull(operation, "operation");
      Objects.requireNonNull(resource, "resource");

      readBarriersPre =
        Set.copyOf(readBarriersPre);
      writeBarriersPre =
        Set.copyOf(writeBarriersPre);
    }
  }

  /**
   * The operation carried out by this resource:
   *
   * <ol>
   *   <li>Waits on {@code layoutWaitsOnWrites} performed by the external operation.</li>
   *   <li>Performs a layout transition.</li>
   *   <li>Has reads {@code readsWaitOnLayout} that must wait on the layout transition.</li>
   *   <li>Has writes {@code writesWaitOnLayout} that must wait on the layout transition.</li>
   *   <li>Executes.</li>
   * </ol>
   *
   * @param operation                The operation
   * @param resource                 The resource
   * @param layoutWaitsOnWrites      The writes upon which the layout transition waits
   * @param operationPerformingWrite The external operation performing writes
   * @param preLayoutFrom            The layout before the transition
   * @param preLayoutTo              The layout after the transition
   * @param readsWaitOnLayout        The reads that must wait on the layout transition
   * @param writesWaitOnLayout       The writes that must wait on the layout transition
   */

  record WithImageLayoutTransitionPre(
    RCGOperationType operation,
    RCGResourceImageType resource,
    Set<RCGCommandPipelineStage> layoutWaitsOnWrites,
    RCGOperationType operationPerformingWrite,
    RCGResourceImageLayout preLayoutFrom,
    RCGResourceImageLayout preLayoutTo,
    Set<RCGCommandPipelineStage> readsWaitOnLayout,
    Set<RCGCommandPipelineStage> writesWaitOnLayout)
    implements RCGResourcePrimitiveBarriersType
  {
    /**
     * With a pre-execution image layout transition.
     */

    public WithImageLayoutTransitionPre
    {
      Objects.requireNonNull(operation, "operation");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(
        operationPerformingWrite,
        "operationPerformingWrite"
      );
      Objects.requireNonNull(preLayoutFrom, "preLayoutFrom");
      Objects.requireNonNull(preLayoutTo, "preLayoutTo");

      layoutWaitsOnWrites =
        Set.copyOf(layoutWaitsOnWrites);
      readsWaitOnLayout =
        Set.copyOf(readsWaitOnLayout);
      writesWaitOnLayout =
        Set.copyOf(writesWaitOnLayout);
    }
  }

  /**
   * The operation carried out by this resource:
   *
   * <ol>
   *   <li>Waits on the given read barriers.</li>
   *   <li>Waits on the given write barriers.</li>
   *   <li>Executes.</li>
   *   <li>Performs a layout transition.</li>
   * </ol>
   *
   * @param operation        The operation
   * @param resource         The resource
   * @param readBarriersPre  The read barriers
   * @param writeBarriersPre The write barriers
   * @param postLayoutFrom   The layout before the transition
   * @param postLayoutTo     The layout after the transition
   */

  record WithImageLayoutTransitionPost(
    RCGOperationType operation,
    RCGResourceImageType resource,
    Set<BlockReadOnExternal> readBarriersPre,
    Set<BlockWriteOnExternal> writeBarriersPre,
    RCGResourceImageLayout postLayoutFrom,
    RCGResourceImageLayout postLayoutTo)
    implements RCGResourcePrimitiveBarriersType
  {
    /**
     * With a post-execution image layout transition.
     */

    public WithImageLayoutTransitionPost
    {
      Objects.requireNonNull(operation, "operation");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(postLayoutFrom, "postLayoutFrom");
      Objects.requireNonNull(postLayoutTo, "postLayoutTo");

      readBarriersPre =
        Set.copyOf(readBarriersPre);
      writeBarriersPre =
        Set.copyOf(writeBarriersPre);
    }
  }

  /**
   * The operation carried out by this resource:
   *
   * <ol>
   *   <li>Waits on {@code layoutWaitsOnWrites} performed by the external operation.</li>
   *   <li>Performs a layout transition.</li>
   *   <li>Has reads {@code readsWaitOnPreLayout} that must wait on the layout transition.</li>
   *   <li>Has writes {@code writesWaitOnPreLayout} that must wait on the layout transition.</li>
   *   <li>Executes.</li>
   *   <li>Performs a layout transition.</li>
   * </ol>
   *
   * @param operation                The operation
   * @param resource                 The resource
   * @param layoutWaitsOnWrites      The writes upon which the layout transition waits
   * @param operationPerformingWrite The external operation performing writes
   * @param preLayoutFrom            The layout before the transition
   * @param preLayoutTo              The layout after the transition
   * @param readsWaitOnPreLayout     The reads that must wait on the layout transition
   * @param writesWaitOnPreLayout    The writes that must wait on the layout transition
   * @param postLayoutTo             The layout after the post transition
   */

  record WithImageLayoutTransitionsPrePost(
    RCGOperationType operation,
    RCGResourceImageType resource,
    Set<RCGCommandPipelineStage> layoutWaitsOnWrites,
    RCGOperationType operationPerformingWrite,
    RCGResourceImageLayout preLayoutFrom,
    RCGResourceImageLayout preLayoutTo,
    Set<RCGCommandPipelineStage> readsWaitOnPreLayout,
    Set<RCGCommandPipelineStage> writesWaitOnPreLayout,
    RCGResourceImageLayout postLayoutTo)
    implements RCGResourcePrimitiveBarriersType
  {
    /**
     * With both pre-execution and post-execution image layout transitions.
     */

    public WithImageLayoutTransitionsPrePost
    {
      Objects.requireNonNull(operation, "operation");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(layoutWaitsOnWrites, "layoutWaitsOnWrites");
      Objects.requireNonNull(
        operationPerformingWrite,
        "operationPerformingWrite"
      );
      Objects.requireNonNull(preLayoutFrom, "preLayoutFrom");
      Objects.requireNonNull(preLayoutTo, "preLayoutTo");
      Objects.requireNonNull(readsWaitOnPreLayout, "readsWaitOnPreLayout");
      Objects.requireNonNull(writesWaitOnPreLayout, "writesWaitOnPreLayout");
      Objects.requireNonNull(postLayoutTo, "postLayoutTo");
    }
  }
}
