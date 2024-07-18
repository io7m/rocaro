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


package com.io7m.rocaro.vanilla.internal.vulkan;

import com.io7m.jcoronado.api.VulkanCallFailedException;
import com.io7m.jcoronado.api.VulkanErrorCodes;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCStrings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.VULKAN_ERROR;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.VULKAN_OPERATION;

/**
 * The type of exceptions related to Vulkan.
 */

public final class RCVulkanException
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

  public RCVulkanException(
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
   * @param cause               The cause
   * @param inAttributes        The attributes
   * @param inErrorCode         The error code
   * @param inRemediatingAction The remediating action
   */

  public RCVulkanException(
    final Throwable cause,
    final Map<String, String> inAttributes,
    final String inErrorCode,
    final Optional<String> inRemediatingAction)
  {
    super(cause, inAttributes, inErrorCode, inRemediatingAction);
  }

  /**
   * Wrap a Vulkan exception.
   *
   * @param e The exception
   *
   * @return A wrapped exception
   */

  public static RCVulkanException wrap(
    final VulkanException e)
  {
    return new RCVulkanException(
      e,
      Map.of(),
      VULKAN.codeName(),
      Optional.empty()
    );
  }

  /**
   * Wrap a Vulkan exception.
   *
   * @param strings The string resources
   * @param e         The exception
   *
   * @return A wrapped exception
   */

  public static RCVulkanException wrap(
    final RCStrings strings,
    final VulkanException e)
  {
    final var attributes = new HashMap<String, String>();
    if (e instanceof final VulkanCallFailedException call) {
      attributes.put(
        strings.format(VULKAN_OPERATION),
        call.function()
      );
      attributes.put(
        strings.format(VULKAN_ERROR),
        VulkanErrorCodes.errorName(call.errorCode())
          .orElse(Integer.toUnsignedString(call.errorCode()))
      );
    }

    return new RCVulkanException(
      e,
      attributes,
      VULKAN.codeName(),
      Optional.empty()
    );
  }
}
