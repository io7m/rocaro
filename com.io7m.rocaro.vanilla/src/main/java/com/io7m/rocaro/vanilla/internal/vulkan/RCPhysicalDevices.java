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
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.rocaro.api.devices.RCDeviceSelectionAny;
import com.io7m.rocaro.api.devices.RCDeviceSelectionType;
import com.io7m.rocaro.vanilla.RCStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions.isSupported;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_DEVICE_NONE_SUITABLE;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.DEVICE_FEATURE_INDEXED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_DEVICE_EXPLICIT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_DEVICE_NO_SUITABLE;

/**
 * Functions to select physical devices.
 */

public final class RCPhysicalDevices
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCPhysicalDevices.class);

  private RCPhysicalDevices()
  {

  }

  /**
   * Select an appropriate physical device.
   *
   * @param strings                The string resources
   * @param instance               The instance
   * @param requiredDeviceFeatures The set of required device features
   * @param deviceSelection        The device selection method
   * @param windowWithSurface      The window with any surfaces attached
   *
   * @return A device
   *
   * @throws RCVulkanException On errors
   */

  public static VulkanPhysicalDeviceType create(
    final RCStrings strings,
    final VulkanInstanceType instance,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures,
    final RCDeviceSelectionType deviceSelection,
    final RCWindowWithSurfaceType windowWithSurface)
    throws RCVulkanException
  {
    showRequiredFeatures(requiredDeviceFeatures);

    return switch (deviceSelection) {
      case final RCDeviceSelectionAny _ ->
        createAny(strings, instance, requiredDeviceFeatures, windowWithSurface);
    };
  }

  private static boolean isDeviceSuitable(
    final VulkanPhysicalDeviceType device,
    final VulkanPhysicalDeviceFeatures requiredFeatures,
    final RCWindowWithSurfaceType windowWithSurface)
    throws VulkanException
  {
    return isDeviceSuitableForSwapChain(device, windowWithSurface)
           && isDeviceSuitableHasGraphicsQueue(device)
           && isDeviceSuitableHasPresentationQueue(device, windowWithSurface)
           && isDeviceSuitableForFeatures(device, requiredFeatures);
  }

  private static boolean isDeviceSuitableHasPresentationQueue(
    final VulkanPhysicalDeviceType device,
    final RCWindowWithSurfaceType windowWithSurface)
    throws VulkanException
  {
    return switch (windowWithSurface) {

      /*
       * Determine which, if any, of the available queues are capable of
       * "presentation". That is, rendering to a surface that will appear
       * onscreen.
       */

      case final RCWindowWithSurface with -> {
        final var deviceName = device.properties().name();

        LOG.debug(
          "Checking device '{}' for presentation support.",
          deviceName);

        final var surfaceExtension =
          with.khrSurfaceExt();
        final var surface =
          with.surface();
        final var presentable =
          surfaceExtension.surfaceSupport(device, surface);

        final var hasQueue = !presentable.isEmpty();
        if (hasQueue) {
          LOG.debug(
            "Physical device '{}' has a presentation queue.",
            deviceName);
        } else {
          LOG.warn(
            "Physical device '{}' is unsuitable: No presentation queues.",
            deviceName
          );
        }

        yield hasQueue;
      }

      /*
       * Offscreen windows do not require a presentation queue.
       */

      case final RCWindowWithoutSurface _ -> {
        yield true;
      }
    };
  }

  private static boolean isDeviceSuitableHasGraphicsQueue(
    final VulkanPhysicalDeviceType device)
    throws VulkanException
  {
    final var hasGraphicsQueue =
      device.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT)
        .isPresent();

    final var deviceName = device.properties().name();
    if (hasGraphicsQueue) {
      LOG.debug(
        "Physical device '{}' has a graphics queue.",
        deviceName
      );
    } else {
      LOG.warn(
        "Physical device '{}' is unsuitable: No graphics queue.",
        deviceName
      );
    }

    return hasGraphicsQueue;
  }

  private static boolean isDeviceSuitableForSwapChain(
    final VulkanPhysicalDeviceType device,
    final RCWindowWithSurfaceType windowWithSurface)
    throws VulkanException
  {
    return switch (windowWithSurface) {

      /*
       * Onscreen windows require swap chain support.
       */

      case final RCWindowWithSurface _ -> {
        final var hasSwapChain =
          device.extensions(Optional.empty())
            .containsKey("VK_KHR_swapchain");

        final var deviceName = device.properties().name();
        if (hasSwapChain) {
          LOG.debug(
            "Physical device '{}' has the VK_KHR_swapchain extension.",
            deviceName
          );
        } else {
          LOG.warn(
            "Physical device '{}' is unsuitable: No VK_KHR_swapchain extension.",
            deviceName
          );
        }

        yield hasSwapChain;
      }

      /*
       * Offscreen windows do not require a swap chain.
       */

      case final RCWindowWithoutSurface _ -> {
        yield true;
      }
    };
  }

  private static boolean isDeviceSuitableForFeatures(
    final VulkanPhysicalDeviceType device,
    final VulkanPhysicalDeviceFeatures requiredFeatures)
    throws VulkanException
  {
    final var supportedFeatures =
      device.features();
    final var unavailable =
      isSupported(supportedFeatures, requiredFeatures);

    final var deviceName = device.properties().name();
    if (!unavailable.isEmpty()) {
      LOG.warn(
        "Physical device '{}' is unsuitable: Unsupported required features: {}",
        deviceName,
        unavailable
      );
      return false;
    }

    LOG.debug(
      "Physical device '{}' has all required hardware features.",
      deviceName
    );
    return true;
  }

  private static VulkanPhysicalDeviceType createAny(
    final RCStrings strings,
    final VulkanInstanceType instance,
    final VulkanPhysicalDeviceFeatures requiredFeatures,
    final RCWindowWithSurfaceType windowWithSurface)
    throws RCVulkanException
  {
    try {
      final var devices = instance.physicalDevices();
      for (final var device : devices) {
        if (isDeviceSuitable(device, requiredFeatures, windowWithSurface)) {
          return device;
        }
      }

      if (devices.isEmpty()) {
        LOG.warn("No Vulkan physical devices were available at all!");
      }

      throw errorNoSuitableDevice(strings, requiredFeatures);
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private static void showRequiredFeatures(
    final VulkanPhysicalDeviceFeatures features)
  {
    final var featureMap =
      VulkanPhysicalDeviceFeaturesFunctions.mapOf(features);

    boolean requiredAny = false;
    for (final var entry : featureMap.entrySet()) {
      final var name = entry.getKey();
      final var required = entry.getValue();
      if (required) {
        LOG.debug("Device feature (Required): {}", name);
        requiredAny = true;
      }
    }
    if (!requiredAny) {
      LOG.debug("No device features are required.");
    }
  }

  private static RCVulkanException errorNoSuitableDevice(
    final RCStrings strings,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures)
  {
    final var attributes =
      new HashMap<String, String>();

    final var required =
      VulkanPhysicalDeviceFeaturesFunctions.mapOf(requiredDeviceFeatures)
        .entrySet()
        .stream()
        .filter(Map.Entry::getValue)
        .toList();

    for (int index = 0; index < required.size(); ++index) {
      final var entry = required.get(index);
      attributes.put(
        strings.format(DEVICE_FEATURE_INDEXED, index),
        entry.getKey()
      );
    }

    return new RCVulkanException(
      strings.format(ERROR_DEVICE_NO_SUITABLE),
      attributes,
      VULKAN_DEVICE_NONE_SUITABLE.codeName(),
      Optional.of(strings.format(ERROR_DEVICE_EXPLICIT))
    );
  }
}
