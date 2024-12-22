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

import com.io7m.rocaro.api.RocaroException;

import java.util.Objects;

/**
 * The status of an operation.
 */

public sealed interface RCGOperationStatusType
{
  /**
   * The operation is not initialized, and no attempt has been made to
   * prepare it.
   */

  enum Uninitialized implements RCGOperationStatusType
  {
    /**
     * The operation is not initialized, and no attempt has been made to
     * prepare it.
     */

    UNINITIALIZED
  }

  /**
   * The operation is currently preparing itself. This typically means the
   * operation is waiting for resources to be allocated and/or loaded.
   *
   * @param message  The status message
   * @param progress The progress
   */

  record Preparing(
    String message,
    double progress)
    implements RCGOperationStatusType
  {
    /**
     * The operation is currently preparing itself. This typically means the
     * operation is waiting for resources to be allocated and/or loaded.
     */

    public Preparing
    {
      Objects.requireNonNull(message, "message");
      progress = Math.clamp(progress, 0.0, 1.0);
    }
  }

  /**
   * The operation is ready.
   */

  enum Ready
    implements RCGOperationStatusType
  {
    /**
     * The operation is ready.
     */

    READY
  }

  /**
   * The operation could not be prepared due to encountering one or more
   * errors.
   *
   * @param exception The exception
   */

  record PreparationFailed(
    RocaroException exception)
    implements RCGOperationStatusType
  {
    /**
     * The operation could not be prepared due to encountering one or more
     * errors.
     */

    public PreparationFailed
    {
      Objects.requireNonNull(exception, "exception");
    }
  }
}
