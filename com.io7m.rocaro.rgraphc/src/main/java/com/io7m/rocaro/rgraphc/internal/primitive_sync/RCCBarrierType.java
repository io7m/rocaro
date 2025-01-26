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
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderType;

import java.util.Set;

/**
 * The base type of commands that are barriers.
 */

public sealed interface RCCBarrierType
  extends RCCSyncCommandType
  permits RCCBarrierWithQueueTransferType,
  RCCImageBarrier,
  RCCImageBarrierWithQueueTransfer,
  RCCMemoryBarrier,
  RCCMemoryBarrierWithQueueTransfer
{
  /**
   * @return The execution that owns the barrier
   */

  RCCExecute owner();

  /**
   * @return The resource
   */

  RCCPPlaceholderType resource();

  /**
   * @return The read stages blocked by this barrier
   */

  @JsonProperty("BlocksReadsAt")
  Set<RCGCommandPipelineStage> blocksReadsAt();

  /**
   * @return The write stages blocked by this barrier
   */

  @JsonProperty("BlocksWritesAt")
  Set<RCGCommandPipelineStage> blocksWritesAt();

  /**
   * @return The stages at which to wait for writes
   */

  @JsonProperty("WaitsForWritesAt")
  Set<RCGCommandPipelineStage> waitsForWritesAt();
}
