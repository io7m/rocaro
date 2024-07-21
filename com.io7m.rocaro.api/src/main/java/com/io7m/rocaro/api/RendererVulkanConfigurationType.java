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


package com.io7m.rocaro.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jcoronado.api.VulkanInstanceProviderType;
import com.io7m.rocaro.api.devices.RCDeviceSelectionAny;
import com.io7m.rocaro.api.devices.RCDeviceSelectionType;
import com.io7m.verona.core.Version;
import org.immutables.value.Value;

import java.time.Duration;

/**
 * Configuration information for Vulkan.
 */

@Value.Immutable
@ImmutablesStyleType
public interface RendererVulkanConfigurationType
{
  /**
   * @return The Vulkan instance provider
   */

  @Value.Parameter
  VulkanInstanceProviderType instanceProvider();

  /**
   * @return The device selection
   */

  @Value.Default
  default RCDeviceSelectionType deviceSelection()
  {
    return new RCDeviceSelectionAny();
  }

  /**
   * When attempting to acquire an image from a window system for rendering,
   * this is the maximum amount of time to wait for an image. This value is
   * intended to prevent deadlocks, but should otherwise be set to a value
   * high enough such that a timeout should not be seen in normal operation.
   * <p>
   * The default value is one second.
   *
   * @return The maximum time to wait for an image
   */

  @Value.Default
  default Duration imageAcquisitionTimeout()
  {
    return Duration.ofSeconds(1L);
  }

  /**
   * @return Enable API validation
   *
   * @see "https://docs.vulkan.org/guide/latest/validation_overview.html"
   */

  @Value.Default
  default boolean enableValidation()
  {
    return false;
  }

  /**
   * @return The application name
   */

  @Value.Default
  default String applicationName()
  {
    return "";
  }

  /**
   * @return The application version
   */

  @Value.Default
  default Version applicationVersion()
  {
    return Version.of(1, 0, 0);
  }
}
