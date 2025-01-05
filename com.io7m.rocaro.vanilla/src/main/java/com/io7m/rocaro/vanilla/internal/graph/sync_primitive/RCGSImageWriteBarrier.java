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
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceSchematicImageType;

import java.util.Objects;

/**
 * An image write barrier.
 */

public final class RCGSImageWriteBarrier
  extends RCGSAbstractCommand
  implements RCGSWriteType, RCGSWriteBarrierType
{
  private final RCGSExecute owner;
  private final RCGResourceVariable<? extends RCResourceSchematicImageType> resource;
  private final RCGCommandPipelineStage waitsForWriteAt;
  private final RCGCommandPipelineStage blocksWriteAt;
  private final RCGResourceImageLayout layoutFrom;
  private final RCGResourceImageLayout layoutTo;

  /**
   * An image write barrier.
   *
   * @param id                The command ID
   * @param inOwner           The command owner
   * @param inResource        The resource
   * @param inBlocksWriteAt   The write access stage that will be blocked
   * @param inWaitsForWriteAt The write access stage that will unblock the barrier
   * @param inLayoutFrom      The source layout
   * @param inLayoutTo        The target layout
   */

  public RCGSImageWriteBarrier(
    final long id,
    final RCGSExecute inOwner,
    final RCGResourceVariable<? extends RCResourceSchematicImageType> inResource,
    final RCGCommandPipelineStage inWaitsForWriteAt,
    final RCGCommandPipelineStage inBlocksWriteAt,
    final RCGResourceImageLayout inLayoutFrom,
    final RCGResourceImageLayout inLayoutTo)
  {
    super(id);

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
  }

  @Override
  public RCGCommandPipelineStage writeStage()
  {
    return this.blocksWriteAt;
  }

  @Override
  public RCGSExecute owner()
  {
    return this.owner;
  }

  @Override
  public RCGResourceVariable<? extends RCResourceSchematicImageType> resource()
  {
    return this.resource;
  }

  @Override
  public RCGCommandPipelineStage waitsForWriteAt()
  {
    return this.waitsForWriteAt;
  }

  @Override
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
}
