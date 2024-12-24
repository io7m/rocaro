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

import com.io7m.rocaro.api.graph.RCGSubmissionID;

import java.util.Objects;

/**
 * The abstract base type of commands.
 */

public abstract class RCGSAbstractCommand
{
  private final long commandId;
  private RCGSubmissionID submission;

  /**
   * The abstract base type of commands.
   *
   * @param inId The command ID
   */

  public RCGSAbstractCommand(
    final long inId)
  {
    this.commandId = inId;
  }

  /**
   * Set the submission to which the command belongs.
   *
   * @param newSubmission The submission
   */

  public final void setSubmission(
    final RCGSubmissionID newSubmission)
  {
    this.submission =
      Objects.requireNonNull(newSubmission, "submission");
  }

  /**
   * @return The command ID
   */

  public final long commandId()
  {
    return this.commandId;
  }

  /**
   * @return The command submission
   */

  public final RCGSubmissionID submission()
  {
    Objects.requireNonNull(this.submission, "submission");
    return this.submission;
  }

  @Override
  public final boolean equals(
    final Object o)
  {
    if (this == o) {
      return true;
    }
    if (!this.getClass().equals(o.getClass())) {
      return false;
    }
    final RCGSAbstractCommand c = (RCGSAbstractCommand) o;
    return this.commandId == c.commandId;
  }

  @Override
  public final int hashCode()
  {
    return Objects.hash(this.commandId);
  }

  @Override
  public final String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Long.toUnsignedString(this.commandId, 16)
    );
  }
}
