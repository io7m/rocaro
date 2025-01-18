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


package com.io7m.rocaro.rgraphc.internal;

import com.io7m.rocaro.api.RocaroException;
import com.io7m.seltzer.api.SStructuredError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RCCompilerException
  extends RocaroException
{
  private final List<SStructuredError<String>> errors;
  private final List<SStructuredError<String>> errorsRead;

  public RCCompilerException(
    final String message,
    final Map<String, String> inAttributes,
    final String inErrorCode,
    final Optional<String> inRemediatingAction,
    final List<SStructuredError<String>> errors)
  {
    super(message, inAttributes, inErrorCode, inRemediatingAction);
    this.errors = new ArrayList<>();
    this.errors.addAll(errors);
    this.errorsRead = Collections.unmodifiableList(this.errors);
  }

  public static RCCompilerException exceptionOf(
    final SStructuredError<String> error)
  {
    return new RCCompilerException(
      error.message(),
      error.attributes(),
      error.errorCode(),
      error.remediatingAction(),
      List.of(error)
    );
  }

  public List<SStructuredError<String>> errors()
  {
    return this.errorsRead;
  }
}
