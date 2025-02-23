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


package com.io7m.rocaro.api.assets;

import com.io7m.rocaro.api.RocaroException;

import java.util.Map;
import java.util.Optional;

/**
 * Exceptions relating to assets.
 */

public final class RCAssetException
  extends RocaroException
{
  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inAttributes        The attributes
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public RCAssetException(
    final String message,
    final Map<String, String> inAttributes,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    super(message, inAttributes, inErrorCode, inRemediatingAction);
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inAttributes        The attributes
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public RCAssetException(
    final String message,
    final Throwable cause,
    final Map<String, String> inAttributes,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    super(message, cause, inAttributes, inErrorCode, inRemediatingAction);
  }

  /**
   * Construct an exception.
   *
   * @param cause               The cause
   * @param inAttributes        The attributes
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public RCAssetException(
    final Throwable cause,
    final Map<String, String> inAttributes,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    super(cause, inAttributes, inErrorCode, inRemediatingAction);
  }
}
