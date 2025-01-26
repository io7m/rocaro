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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.typed.RCTCommentableType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;

import java.util.Comparator;

/**
 * The base type of commands.
 */

@JsonPropertyOrder(alphabetic = true)
public sealed interface RCCCommandType
  extends RCTCommentableType
  permits RCCMetaCommandType, RCCSyncCommandType
{
  /**
   * @return The command kind
   */

  @JsonProperty("Kind")
  default String kind()
  {
    return this.getClass().getSimpleName();
  }

  /**
   * @return The unique-within-a-graph command ID
   */

  @JsonProperty("ID")
  long commandId();

  /**
   * @return The queue submission to which the command belongs
   */

  @JsonProperty("Submission")
  RCGSubmissionID submission();

  /**
   * @return The operation that owns the command
   */

  @JsonIgnore
  RCTOperationDeclaration operation();

  /**
   * Set the queue submission for the command.
   *
   * @param submission The queue submission
   */

  void setSubmission(
    RCGSubmissionID submission);

  /**
   * @return The name of the operation that owns the command
   */

  @JsonProperty("Operation")
  default RCCName operationName()
  {
    return this.operation().name();
  }

  /**
   * @return A comparator that compares by command ID
   */

  static <T extends RCCCommandType> Comparator<T> idComparator()
  {
    return (o1, o2) -> Long.compareUnsigned(o1.commandId(), o2.commandId());
  }
}
