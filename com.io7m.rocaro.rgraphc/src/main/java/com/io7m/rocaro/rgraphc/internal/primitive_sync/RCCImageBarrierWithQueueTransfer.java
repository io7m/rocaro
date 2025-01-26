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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderImageType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * An image barrier including a queue transfer.
 */

public final class RCCImageBarrierWithQueueTransfer
  extends RCCAbstractCommand
  implements RCCBarrierType, RCCBarrierWithQueueTransferType
{
  private final RCCExecute owner;
  private final RCCPPlaceholderImageType resource;
  private final Set<RCGCommandPipelineStage> waitsForWritesAt;
  private final Set<RCGCommandPipelineStage> blocksWritesAt;
  private final Set<RCGCommandPipelineStage> blocksReadsAt;
  private final RCGResourceImageLayout layoutFrom;
  private final RCGResourceImageLayout layoutTo;
  private final RCGSubmissionID queueSource;
  private final RCGSubmissionID queueTarget;

  /**
   * An image barrier including a queue transfer.
   *
   * @param inId              The command ID
   * @param inOwner           The command owner
   * @param inResource        The resource
   * @param inBlocksWritesAt  The write access stages that will be blocked
   * @param inBlocksReadsAt   The read access stages that will be blocked
   * @param inWaitsForWriteAt The write access stages that will unblock the barrier
   * @param inLayoutFrom      The source layout
   * @param inLayoutTo        The target layout
   * @param inQueueSource     The source queue category
   * @param inQueueTarget     The target queue category
   */

  public RCCImageBarrierWithQueueTransfer(
    final long inId,
    final RCCExecute inOwner,
    final RCCPPlaceholderImageType inResource,
    final Set<RCGCommandPipelineStage> inWaitsForWriteAt,
    final Set<RCGCommandPipelineStage> inBlocksWritesAt,
    final Set<RCGCommandPipelineStage> inBlocksReadsAt,
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

    this.waitsForWritesAt =
      Collections.unmodifiableSortedSet(new TreeSet<>(inWaitsForWriteAt));
    this.blocksWritesAt =
      Collections.unmodifiableSortedSet(new TreeSet<>(inBlocksWritesAt));
    this.blocksReadsAt =
      Collections.unmodifiableSortedSet(new TreeSet<>(inBlocksReadsAt));

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
  public RCCExecute owner()
  {
    return this.owner;
  }

  @Override
  public RCCPPlaceholderImageType resource()
  {
    return this.resource;
  }

  @Override
  public Set<RCGCommandPipelineStage> waitsForWritesAt()
  {
    return this.waitsForWritesAt;
  }

  @Override
  public Set<RCGCommandPipelineStage> blocksReadsAt()
  {
    return this.blocksReadsAt;
  }

  @Override
  public Set<RCGCommandPipelineStage> blocksWritesAt()
  {
    return this.blocksWritesAt;
  }

  /**
   * @return The source layout
   */

  @JsonProperty("ImageLayoutFrom")
  public RCGResourceImageLayout layoutFrom()
  {
    return this.layoutFrom;
  }

  /**
   * @return The target layout
   */

  @JsonProperty("ImageLayoutTo")
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

  /**
   * @return The resource name
   */

  @JsonProperty("Resource")
  public long resourceId()
  {
    return this.resource.id();
  }

  @Override
  public RCTOperationDeclaration operation()
  {
    return this.owner.operation();
  }
}
