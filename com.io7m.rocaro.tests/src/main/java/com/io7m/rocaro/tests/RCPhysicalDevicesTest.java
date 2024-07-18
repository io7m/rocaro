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
import com.io7m.jcoronado.api.VulkanInstanceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures10;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceProperties;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueFamilyProperties;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanVersion;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType.VulkanKHRSurfaceType;
import com.io7m.rocaro.api.devices.RCDeviceSelectionAny;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.vulkan.RCPhysicalDevices;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCWindowWithSurface;
import com.io7m.rocaro.vanilla.internal.windows.RCWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.jcoronado.api.VulkanPhysicalDevicePropertiesType.Type.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_DEVICE_NONE_SUITABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RCPhysicalDevicesTest
{
  private static final VulkanExtensionProperties VK_KHR_SWAPCHAIN =
    VulkanExtensionProperties.of("VK_KHR_swapchain", 1);

  private static final VulkanPhysicalDeviceFeatures REQUIRED_DEVICE_FEATURES =
    VulkanPhysicalDeviceFeaturesFunctions.none()
      .withFeatures10(
        VulkanPhysicalDeviceFeatures10.builder()
          .from(VulkanPhysicalDeviceFeaturesFunctions.none().features10())
          .setGeometryShader(true)
          .build()
      );

  private RCStrings strings;
  private RCGLFWFacadeType glfw;
  private VulkanExtKHRSurfaceType surfaceExt;
  private VulkanKHRSurfaceType surface;
  private RCWindowWithSurface window;
  private TreeMap<VulkanQueueFamilyIndex, VulkanQueueFamilyProperties> queueFamilies;
  private ArrayList<VulkanQueueType> queuesCreated;
  private VulkanInstanceType instance;
  private VulkanPhysicalDeviceType device0;

  @BeforeEach
  public void setup()
    throws VulkanException
  {
    this.queueFamilies =
      new TreeMap<>();
    this.queuesCreated =
      new ArrayList<>();

    this.instance =
      mock(VulkanInstanceType.class, Answers.RETURNS_DEEP_STUBS);
    this.device0 =
      mock(VulkanPhysicalDeviceType.class, Answers.RETURNS_DEEP_STUBS);

    when(this.device0.properties())
      .thenReturn(VulkanPhysicalDeviceProperties.of(
        "Example0",
        VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU,
        0,
        0xaaaa,
        VulkanVersion.of(1, 3, 0),
        VulkanVersion.of(1, 0, 0)
      ));

    this.strings =
      new RCStrings(Locale.ROOT);
    this.glfw =
      mock(RCGLFWFacadeType.class);
    this.surfaceExt =
      mock(VulkanExtKHRSurfaceType.class);
    this.surface =
      mock(VulkanKHRSurfaceType.class);
    this.window =
      new RCWindowWithSurface(
        new RCWindow("X", 0L, this.glfw),
        this.surfaceExt,
        this.surface
      );
  }

  /**
   * An implementation that doesn't provide any physical devices must fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyErrorNoPhysicalDevices()
    throws Exception
  {
    when(this.instance.physicalDevices())
      .thenReturn(List.of());

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCPhysicalDevices.create(
          this.strings,
          this.instance,
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          new RCDeviceSelectionAny(),
          this.window
        );
      });

    assertEquals(VULKAN_DEVICE_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  /**
   * A physical device that meets all requirements is selected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyPhysicalDeviceMeetsAll()
    throws Exception
  {
    final var queueFamily0 =
      VulkanQueueFamilyProperties.builder()
        .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
        .setQueueCount(1)
        .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
        .addQueueFlags(VK_QUEUE_GRAPHICS_BIT)
        .setTimestampValidBits(32)
        .build();

    when(this.instance.physicalDevices())
      .thenReturn(List.of(this.device0));

    when(this.device0.features())
      .thenReturn(REQUIRED_DEVICE_FEATURES);

    when(this.device0.extensions(Optional.empty()))
      .thenReturn(Map.of("VK_KHR_swapchain", VK_KHR_SWAPCHAIN));

    when(this.device0.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT))
      .thenReturn(Optional.of(queueFamily0));

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(queueFamily0));

    final var device =
      RCPhysicalDevices.create(
        this.strings,
        this.instance,
        REQUIRED_DEVICE_FEATURES,
        new RCDeviceSelectionAny(),
        this.window
      );

    assertSame(this.device0, device);
  }

  /**
   * A physical device that fails to meet requirements is not selected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyPhysicalDeviceMissingFeatures()
    throws Exception
  {
    final var queueFamily0 =
      VulkanQueueFamilyProperties.builder()
        .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
        .setQueueCount(1)
        .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
        .addQueueFlags(VK_QUEUE_GRAPHICS_BIT)
        .setTimestampValidBits(32)
        .build();

    when(this.instance.physicalDevices())
      .thenReturn(List.of(this.device0));

    when(this.device0.features())
      .thenReturn(VulkanPhysicalDeviceFeaturesFunctions.none());

    when(this.device0.extensions(Optional.empty()))
      .thenReturn(Map.of("VK_KHR_swapchain", VK_KHR_SWAPCHAIN));

    when(this.device0.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT))
      .thenReturn(Optional.of(queueFamily0));

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(queueFamily0));

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCPhysicalDevices.create(
          this.strings,
          this.instance,
          REQUIRED_DEVICE_FEATURES,
          new RCDeviceSelectionAny(),
          this.window
        );
      });

    assertEquals(VULKAN_DEVICE_NONE_SUITABLE.codeName(), ex.errorCode());
    assertEquals("GeometryShader", ex.attributes().get("Device Feature [0]"));
  }

  /**
   * A physical device that fails to meet requirements is not selected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyPhysicalDeviceMissingQueue()
    throws Exception
  {
    final var queueFamily0 =
      VulkanQueueFamilyProperties.builder()
        .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
        .setQueueCount(1)
        .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
        .addQueueFlags(VK_QUEUE_GRAPHICS_BIT)
        .setTimestampValidBits(32)
        .build();

    when(this.instance.physicalDevices())
      .thenReturn(List.of(this.device0));

    when(this.device0.features())
      .thenReturn(REQUIRED_DEVICE_FEATURES);

    when(this.device0.extensions(Optional.empty()))
      .thenReturn(Map.of("VK_KHR_swapchain", VK_KHR_SWAPCHAIN));

    when(this.device0.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT))
      .thenReturn(Optional.empty());

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(queueFamily0));

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCPhysicalDevices.create(
          this.strings,
          this.instance,
          REQUIRED_DEVICE_FEATURES,
          new RCDeviceSelectionAny(),
          this.window
        );
      });

    assertEquals(VULKAN_DEVICE_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  /**
   * A physical device that fails to meet requirements is not selected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyPhysicalDeviceMissingSwapChain()
    throws Exception
  {
    final var queueFamily0 =
      VulkanQueueFamilyProperties.builder()
        .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
        .setQueueCount(1)
        .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
        .addQueueFlags(VK_QUEUE_GRAPHICS_BIT)
        .setTimestampValidBits(32)
        .build();

    when(this.instance.physicalDevices())
      .thenReturn(List.of(this.device0));

    when(this.device0.features())
      .thenReturn(REQUIRED_DEVICE_FEATURES);

    when(this.device0.extensions(Optional.empty()))
      .thenReturn(Map.of());

    when(this.device0.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT))
      .thenReturn(Optional.of(queueFamily0));

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(queueFamily0));

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCPhysicalDevices.create(
          this.strings,
          this.instance,
          REQUIRED_DEVICE_FEATURES,
          new RCDeviceSelectionAny(),
          this.window
        );
      });

    assertEquals(VULKAN_DEVICE_NONE_SUITABLE.codeName(), ex.errorCode());
  }

  /**
   * A physical device that fails to meet requirements is not selected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAnyPhysicalDeviceMissingPresentationQueues()
    throws Exception
  {
    final var queueFamily0 =
      VulkanQueueFamilyProperties.builder()
        .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
        .setQueueCount(1)
        .setMinImageTransferGranularity(VulkanExtent3D.of(1, 1, 1))
        .addQueueFlags(VK_QUEUE_GRAPHICS_BIT)
        .setTimestampValidBits(32)
        .build();

    when(this.instance.physicalDevices())
      .thenReturn(List.of(this.device0));

    when(this.device0.features())
      .thenReturn(REQUIRED_DEVICE_FEATURES);

    when(this.device0.extensions(Optional.empty()))
      .thenReturn(Map.of("VK_KHR_swapchain", VK_KHR_SWAPCHAIN));

    when(this.device0.queueFamilyFindWithFlags(VK_QUEUE_GRAPHICS_BIT))
      .thenReturn(Optional.of(queueFamily0));

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of());

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCPhysicalDevices.create(
          this.strings,
          this.instance,
          REQUIRED_DEVICE_FEATURES,
          new RCDeviceSelectionAny(),
          this.window
        );
      });

    assertEquals(VULKAN_DEVICE_NONE_SUITABLE.codeName(), ex.errorCode());
  }
}
