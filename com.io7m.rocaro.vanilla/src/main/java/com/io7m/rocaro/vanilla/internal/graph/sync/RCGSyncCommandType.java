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

import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGSubmissionID;

/**
 * The base type of synchronization commands.
 */

public sealed interface RCGSyncCommandType
  permits RCGSBarrierType,
  RCGSReadType,
  RCGSWriteType,
  RCGSExecute
{
  /**
   * @return The unique-within-a-graph command ID
   */

  long commandId();

  /**
   * @return The queue submission to which the command belongs
   */

  RCGSubmissionID submission();

  /**
   * @return The operation that owns the command
   */

  RCGOperationType operation();

  /**
   * Set the queue submission for the command.
   *
   * @param submission The queue submission
   */

  void setSubmission(
    RCGSubmissionID submission);
}
