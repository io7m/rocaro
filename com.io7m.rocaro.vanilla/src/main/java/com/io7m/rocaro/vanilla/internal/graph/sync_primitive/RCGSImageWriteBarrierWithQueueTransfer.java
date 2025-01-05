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


package com.io7m.rocaro.vanilla.internal.graph.sync_primitive;

import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.resources.RCResourceSchematicImageType;

import java.util.Objects;

/**
 * An image write barrier including a queue transfer.
 */

public final class RCGSImageWriteBarrierWithQueueTransfer
  extends RCGSAbstractCommand
  implements RCGSWriteType,
  RCGSWriteBarrierType,
  RCGSBarrierWithQueueTransferType
{
  private final RCGSExecute owner;
  private final RCGResourceVariable<? extends RCResourceSchematicImageType> resource;
  private final RCGCommandPipelineStage waitsForWriteAt;
  private final RCGCommandPipelineStage blocksWriteAt;
  private final RCGResourceImageLayout layoutFrom;
  private final RCGResourceImageLayout layoutTo;
  private final RCGSubmissionID queueSource;
  private final RCGSubmissionID queueTarget;

  /**
   * An image write barrier including a queue transfer.
   *
   * @param inId              The command ID
   * @param inOwner           The command owner
   * @param inResource        The resource
   * @param inBlocksWriteAt   The read access stage that will be blocked
   * @param inWaitsForWriteAt The write access stage that will unblock the barrier
   * @param inLayoutFrom      The source layout
   * @param inLayoutTo        The target layout
   * @param inQueueSource     The source queue category
   * @param inQueueTarget     The target queue category
   */

  public RCGSImageWriteBarrierWithQueueTransfer(
    final long inId,
    final RCGSExecute inOwner,
    final RCGResourceVariable<? extends RCResourceSchematicImageType> inResource,
    final RCGCommandPipelineStage inWaitsForWriteAt,
    final RCGCommandPipelineStage inBlocksWriteAt,
    final RCGResourceImageLayout inLayoutFrom,
    final RCGResourceImageLayout inLayoutTo,
    final RCGSubmissionID inQueueSource,
    final RCGSubmissionID inQueueTarget)
  {
    super(inId);

    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.resource =
      Objects.requireNonNull(inResource, "resource");
    this.waitsForWriteAt =
      Objects.requireNonNull(inWaitsForWriteAt, "waitsForWriteAt");
    this.blocksWriteAt =
      Objects.requireNonNull(inBlocksWriteAt, "blocksWriteAt");
    this.layoutFrom =
      Objects.requireNonNull(inLayoutFrom, "layoutFrom");
    this.layoutTo =
      Objects.requireNonNull(inLayoutTo, "layoutTo");
    this.queueSource =
      Objects.requireNonNull(inQueueSource, "queueSource");
    this.queueTarget =
      Objects.requireNonNull(inQueueTarget, "queueTarget");
  }

  @Override
  public RCGSExecute owner()
  {
    return this.owner;
  }

  @Override
  public RCGCommandPipelineStage writeStage()
  {
    return this.blocksWriteAt;
  }

  @Override
  public RCGResourceVariable<? extends RCResourceSchematicImageType> resource()
  {
    return this.resource;
  }

  /**
   * @return The stage at which the barrier will wait for writes
   */

  public RCGCommandPipelineStage waitsForWriteAt()
  {
    return this.waitsForWriteAt;
  }

  /**
   * @return The write access that will be blocked until this barrier completes
   */

  public RCGCommandPipelineStage blocksWriteAt()
  {
    return this.blocksWriteAt;
  }

  /**
   * @return The source layout
   */

  public RCGResourceImageLayout layoutFrom()
  {
    return this.layoutFrom;
  }

  /**
   * @return The target layout
   */

  public RCGResourceImageLayout layoutTo()
  {
    return this.layoutTo;
  }

  @Override
  public RCGSubmissionID queueSource()
  {
    return this.queueSource;
  }

  @Override
  public RCGSubmissionID queueTarget()
  {
    return this.queueTarget;
  }
}
