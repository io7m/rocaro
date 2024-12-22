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
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A pseudo-command representing the execution of an operation. Note that
 * this is essentially a readability aid for large graphs; the actual effects
 * of the operation are represented by the associated reads and writes.
 */

public final class RCGSExecute
  extends RCGSAbstractCommand
  implements RCGSyncCommandType
{
  private final Set<RCGSRead> reads;
  private final Set<RCGSWrite> writes;
  private final RCGOperationType operation;
  private final Set<RCGSRead> readsR;
  private final Set<RCGSWrite> writesR;

  RCGSExecute(
    final long id,
    final RCGOperationType inOperation)
  {
    super(id);

    this.operation =
      Objects.requireNonNull(inOperation, "operation");
    this.reads =
      new HashSet<>();
    this.writes =
      new HashSet<>();
    this.readsR =
      Collections.unmodifiableSet(this.reads);
    this.writesR =
      Collections.unmodifiableSet(this.writes);
  }

  @Override
  public RCGOperationType operation()
  {
    return this.operation;
  }

  /**
   * @return The read commands associated with the execution
   */

  public Set<RCGSRead> reads()
  {
    return this.readsR;
  }

  /**
   * @return The write commands associated with the execution
   */

  public Set<RCGSWrite> writes()
  {
    return this.writesR;
  }

  /**
   * Add a read command.
   *
   * @param id       The ID
   * @param resource The resource
   * @param stage    The read stage
   *
   * @return The command
   */

  public RCGSRead addRead(
    final long id,
    final RCGResourcePlaceholderType resource,
    final RCGCommandPipelineStage stage)
  {
    final var r = new RCGSRead(id, this, resource, stage);
    this.reads.add(r);
    return r;
  }

  /**
   * Add a write command.
   *
   * @param id       The ID
   * @param resource The resource
   * @param stage    The write stage
   *
   * @return The command
   */

  public RCGSWrite addWrite(
    final long id,
    final RCGResourcePlaceholderType resource,
    final RCGCommandPipelineStage stage)
  {
    final var w = new RCGSWrite(id, this, resource, stage);
    this.writes.add(w);
    return w;
  }
}
