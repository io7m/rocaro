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


package com.io7m.rocaro.tests;

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtensionProperties;
import com.io7m.jcoronado.api.VulkanExtent3D;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceProperties;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueFamilyProperties;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanVersion;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.fake.VFakeInstances;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.io7m.jcoronado.api.VulkanPhysicalDevicePropertiesType.Type.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public final class RCFakeVulkan
{
  private static final VulkanExtensionProperties VK_KHR_SURFACE =
    VulkanExtensionProperties.of("VK_KHR_surface", 1);

  private static final VulkanExtensionProperties VK_KHR_SWAPCHAIN =
    VulkanExtensionProperties.of("VK_KHR_swapchain", 1);

  public static VFakeInstances fake()
  {
    final VFakeInstances instances =
      new VFakeInstances();

    instances.setVulkanVersion(VulkanVersion.of(1,3,0));
    return instances;
  }

  public static VulkanLogicalDeviceType fakeLogicalDevice()
  {
    try {
      final VulkanLogicalDeviceType device =
        Mockito.mock(
          VulkanLogicalDeviceType.class,
          Answers.RETURNS_DEEP_STUBS
        );

      final var queue =
        Mockito.mock(VulkanQueueType.class, Answers.RETURNS_DEEP_STUBS);

      when(device.queues())
        .thenReturn(List.of(queue));

      return device;
    } catch (final VulkanException e) {
      throw new IllegalStateException(e);
    }
  }

  public static VulkanPhysicalDeviceType fakePhysicalDevice()
  {
    try {
      final VulkanPhysicalDeviceType device =
        Mockito.mock(
          VulkanPhysicalDeviceType.class,
          Answers.RETURNS_DEEP_STUBS
        );

      final VulkanPhysicalDeviceProperties properties =
        VulkanPhysicalDeviceProperties.of(
          "Fake",
          VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU,
          0x1000,
          0x1000,
          VulkanVersion.of(1, 3, 0),
          VulkanVersion.of(1, 0, 0)
        );

      final var queues =
        new TreeMap<VulkanQueueFamilyIndex, VulkanQueueFamilyProperties>();

      final var mainQueue =
        VulkanQueueFamilyProperties.builder()
          .addQueueFlags(VulkanQueueFamilyPropertyFlag.values())
          .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
          .setQueueCount(1)
          .setTimestampValidBits(32)
          .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
          .build();

      queues.put(new VulkanQueueFamilyIndex(0), mainQueue);

      when(device.queueFamilyFindWithFlags((Set<VulkanQueueFamilyPropertyFlag>) any()))
        .thenReturn(Optional.of(mainQueue));

      when(device.queueFamilyFindWithFlags((VulkanQueueFamilyPropertyFlag) any()))
        .thenReturn(Optional.of(mainQueue));

      when(device.queueFamilies())
        .thenReturn(queues);

      when(device.properties())
        .thenReturn(properties);

      when(device.extensions(Optional.empty()))
        .thenReturn(Map.ofEntries(
          Map.entry(VK_KHR_SWAPCHAIN.name(), VK_KHR_SWAPCHAIN)
        ));

      return device;
    } catch (final VulkanException e) {
      throw new IllegalStateException(e);
    }
  }

  public static VulkanExtKHRSurfaceType fakeKHRSurface()
  {
    return Mockito.mock(
      VulkanExtKHRSurfaceType.class,
      Answers.RETURNS_DEEP_STUBS
    );
  }
}
