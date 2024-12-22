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
 * A read operation.
 */

public final class RCGSRead
  extends RCGSAbstractCommand
  implements RCGSReadType
{
  private final RCGSExecute owner;
  private final RCGResourcePlaceholderType resource;
  private final RCGCommandPipelineStage readsAt;

  /**
   * A read operation.
   *
   * @param id         The ID
   * @param inOwner    The executing command
   * @param inResource The resource
   * @param inReadsAt  The stage at which the read access occurs
   */

  public RCGSRead(
    final long id,
    final RCGSExecute inOwner,
    final RCGResourcePlaceholderType inResource,
    final RCGCommandPipelineStage inReadsAt)
  {
    super(id);

    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.resource =
      Objects.requireNonNull(inResource, "resource");
    this.readsAt =
      Objects.requireNonNull(inReadsAt, "readsAt");

  }

  @Override
  public RCGSExecute owner()
  {
    return this.owner;
  }

  /**
   * @return The resource
   */

  public RCGResourcePlaceholderType resource()
  {
    return this.resource;
  }

  /**
   * @return The stage at which the read occurs
   */

  public RCGCommandPipelineStage readsAt()
  {
    return this.readsAt;
  }
}
