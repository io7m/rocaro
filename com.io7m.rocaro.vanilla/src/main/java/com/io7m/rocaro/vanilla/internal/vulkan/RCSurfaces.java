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

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanInstanceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.windows.RCWindow;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowFullscreen;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowOffscreen;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;

import java.util.Map;
import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.EXTENSION;

/**
 * Functions to allocate surfaces.
 */

public final class RCSurfaces
{
  private RCSurfaces()
  {

  }

  /**
   * Create a surface from an existing window.
   *
   * @param strings  The string resources
   * @param instance The Vulkan instance
   * @param window   The window
   *
   * @return A window with a surface
   *
   * @throws RCVulkanException On errors
   */

  public static RCWindowWithSurfaceType createWindowWithSurface(
    final RCStrings strings,
    final VulkanInstanceType instance,
    final RCWindowType window)
    throws RCVulkanException
  {
    try {
      return switch (window) {
        case final RCWindow windowed -> {
          final var khrSurfaceExt =
            findSurfaceExtension(strings, instance);
          final var surface =
            khrSurfaceExt.surfaceFromWindow(instance, windowed.address());

          yield new RCWindowWithSurface(windowed, khrSurfaceExt, surface);
        }

        case final RCWindowFullscreen fullscreen -> {
          final var khrSurfaceExt =
            findSurfaceExtension(strings, instance);
          final var surface =
            khrSurfaceExt.surfaceFromWindow(instance, fullscreen.address());

          yield new RCWindowWithSurface(fullscreen, khrSurfaceExt, surface);
        }

        case final RCWindowOffscreen _ -> {
          yield new RCWindowWithoutSurface(window);
        }
      };
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(strings, e);
    }
  }

  private static VulkanExtKHRSurfaceType findSurfaceExtension(
    final RCStrings strings,
    final VulkanInstanceType instance)
    throws RCVulkanException, VulkanException
  {
    return instance.findEnabledExtension(
        "VK_KHR_surface",
        VulkanExtKHRSurfaceType.class)
      .orElseThrow(() -> {
        return errorMissingRequiredExtension(strings, "VK_KHR_surface");
      });
  }

  private static RCVulkanException errorMissingRequiredExtension(
    final RCStrings strings,
    final String extension)
  {
    return new RCVulkanException(
      strings.format(ERROR_VULKAN_EXTENSION_MISSING),
      Map.ofEntries(
        Map.entry(
          strings.format(EXTENSION),
          extension
        )
      ),
      VULKAN_EXTENSION_MISSING.codeName(),
      Optional.of(strings.format(ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION))
    );
  }
}
