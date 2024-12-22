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


package com.io7m.rocaro.vanilla.internal.graph.sync;

import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;

import java.util.Objects;

/**
 * A memory write barrier.
 */

public final class RCGSMemoryWriteBarrier
  extends RCGSAbstractCommand
  implements RCGSWriteType, RCGSWriteBarrierType
{
  private final RCGSExecute owner;
  private final RCGResourcePlaceholderType resource;
  private final RCGCommandPipelineStage waitsForWriteAt;
  private final RCGCommandPipelineStage blocksWriteAt;

  /**
   * A memory write barrier.
   *
   * @param id                The command ID
   * @param inOwner           The command owner
   * @param inResource        The resource
   * @param inBlocksWriteAt   The write access stage that will be blocked
   * @param inWaitsForWriteAt The write access stage that will unblock the barrier
   */

  public RCGSMemoryWriteBarrier(
    final long id,
    final RCGSExecute inOwner,
    final RCGResourcePlaceholderType inResource,
    final RCGCommandPipelineStage inWaitsForWriteAt,
    final RCGCommandPipelineStage inBlocksWriteAt)
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
  public RCGResourcePlaceholderType resource()
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
}
