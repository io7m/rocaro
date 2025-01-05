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
import com.io7m.rocaro.api.graph.RCGResourceVariable;

import java.util.Objects;

/**
 * A write command.
 */

public final class RCGSWrite
  extends RCGSAbstractCommand
  implements RCGSWriteType
{
  private final RCGSExecute owner;
  private final RCGResourceVariable<?> resource;
  private final RCGCommandPipelineStage writesAt;

  /**
   * A write command.
   *
   * @param id         The ID
   * @param inOwner    The executing command
   * @param inResource The resource
   * @param inWritesAt The stage at which the write command occurs
   */

  public RCGSWrite(
    final long id,
    final RCGSExecute inOwner,
    final RCGResourceVariable<?> inResource,
    final RCGCommandPipelineStage inWritesAt)
  {
    super(id);

    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.resource =
      Objects.requireNonNull(inResource, "resource");
    this.writesAt =
      Objects.requireNonNull(inWritesAt, "writesAt");
  }

  @Override
  public RCGCommandPipelineStage writeStage()
  {
    return this.writesAt;
  }

  @Override
  public RCGSExecute owner()
  {
    return this.owner;
  }

  /**
   * @return The resource
   */

  public RCGResourceVariable<?> resource()
  {
    return this.resource;
  }

  /**
   * @return The stage at which the write command occurs
   */

  public RCGCommandPipelineStage writesAt()
  {
    return this.writesAt;
  }
}
