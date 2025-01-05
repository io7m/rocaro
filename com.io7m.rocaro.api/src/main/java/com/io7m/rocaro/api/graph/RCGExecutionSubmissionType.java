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


package com.io7m.rocaro.api.graph;

import java.util.List;
import java.util.Set;

/**
 * A submission of work to a queue as part of execution.
 */

public interface RCGExecutionSubmissionType
{
  /**
   * @return The submission ID
   */

  RCGSubmissionID submissionId();

  /**
   * @return The items in execution order
   */

  List<RCGExecutionItemType> items();

  /**
   * @return The set of semaphores upon which to wait before executing the submission
   */

  Set<RCGSemaphoreBinaryType> waitSemaphores();

  /**
   * @return The set of semaphores to signal after execution of the submission has completed
   */

  Set<RCGSemaphoreBinaryType> signalSemaphores();

  /**
   * @return The queue transfer operations to perform before starting
   */

  List<RCGQueueTransferType> startingQueueTransfers();

  /**
   * @return The queue transfer operations to perform after ending
   */

  List<RCGQueueTransferType> endingQueueTransfers();
}
