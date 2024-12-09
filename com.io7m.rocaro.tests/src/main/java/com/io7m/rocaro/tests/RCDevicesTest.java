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

import com.io7m.jcoronado.api.VulkanDebuggingType;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent2D;
import com.io7m.jcoronado.api.VulkanExtent3D;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceQueueCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueFamilyProperties;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.api.VulkanQueueIndex;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSemaphoreBinaryType;
import com.io7m.jcoronado.api.VulkanSemaphoreType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType.VulkanKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanSurfaceCapabilitiesKHR;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType.VulkanKHRSwapChainType;
import com.io7m.jcoronado.fake.VFakeInstances;
import com.io7m.jcoronado.vma.VMAAllocatorProviderType;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.threading.RCStandardExecutors;
import com.io7m.rocaro.vanilla.internal.vulkan.RCLogicalDevices;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCWindowWithSurface;
import com.io7m.rocaro.vanilla.internal.windows.RCWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_COMPUTE_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_SPARSE_BINDING_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_TRANSFER_BIT;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_QUEUE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class RCDevicesTest
{
  private RCStrings strings;
  private RCGLFWFacadeType glfw;
  private VulkanExtKHRSurfaceType surfaceExt;
  private VulkanKHRSurfaceType surface;
  private VulkanLogicalDeviceType logicalDevice;
  private VulkanPhysicalDeviceType physicalDevice;
  private RCWindowWithSurface window;
  private TreeMap<VulkanQueueFamilyIndex, VulkanQueueFamilyProperties> queueFamilies;
  private ArrayList<VulkanQueueType> queuesCreated;
  private VulkanLogicalDeviceCreateInfo deviceCreateInfoLogged;
  private VulkanExtKHRSwapChainType swapchainExt;
  private VulkanKHRSwapChainType swapChain;
  private VulkanImageType swapChainImage;
  private VulkanImageViewType swapChainImageView;
  private VulkanSemaphoreBinaryType imageReadySemaphore;
  private VulkanSemaphoreBinaryType imageRenderDoneSemaphore;
  private VulkanFenceType imageRenderDoneFence;
  private RendererVulkanConfiguration vulkanConfiguration;
  private VMAAllocatorProviderType allocators;
  private VMAAllocatorType allocator;
  private VulkanDebuggingType debugging;
  private RCStandardExecutors executors;

  @BeforeEach
  public void setup()
    throws VulkanException, RCVulkanException
  {
    this.debugging =
      mock(VulkanDebuggingType.class);
    this.allocators =
      mock(VMAAllocatorProviderType.class);
    this.allocator =
      mock(VMAAllocatorType.class);

    when(this.allocators.createAllocator(any()))
      .thenReturn(this.allocator);

    this.vulkanConfiguration =
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(new VFakeInstances())
        .setVmaAllocators(this.allocators)
        .build();

    this.queueFamilies =
      new TreeMap<>();
    this.queuesCreated =
      new ArrayList<>();
    this.surfaceExt =
      mock(VulkanExtKHRSurfaceType.class);
    this.swapchainExt =
      mock(VulkanExtKHRSwapChainType.class);
    this.surface =
      mock(VulkanKHRSurfaceType.class);
    this.swapChain =
      mock(VulkanExtKHRSwapChainType.VulkanKHRSwapChainType.class);

    this.imageReadySemaphore =
      mock(VulkanSemaphoreBinaryType.class);
    this.imageRenderDoneSemaphore =
      mock(VulkanSemaphoreBinaryType.class);
    this.imageRenderDoneFence =
      mock(VulkanFenceType.class);

    when(this.surfaceExt.surfaceCapabilities(any(), any()))
      .thenReturn(
        VulkanSurfaceCapabilitiesKHR.builder()
          .setCurrentExtent(VulkanExtent2D.of(640, 480))
          .setMaxImageArrayLayers(1)
          .setMaxImageCount(0)
          .setMaxImageExtent(VulkanExtent2D.of(640, 480))
          .setMinImageCount(1)
          .setMinImageExtent(VulkanExtent2D.of(640, 480))
          .build()
      );

    this.swapChainImage =
      Mockito.mock(VulkanImageType.class);
    this.swapChainImageView =
      Mockito.mock(VulkanImageViewType.class);

    when(this.swapchainExt.swapChainCreate(any(), any()))
      .thenReturn(this.swapChain);
    when(this.swapChain.images())
      .thenReturn(List.of(this.swapChainImage));

    this.physicalDevice =
      mock(VulkanPhysicalDeviceType.class);
    this.logicalDevice =
      mock(VulkanLogicalDeviceType.class);

    when(this.physicalDevice.createLogicalDevice(any()))
      .thenAnswer(invocationOnMock -> {
        this.deviceCreateInfoLogged = invocationOnMock.getArgument(0);
        return this.logicalDevice;
      });
    when(this.physicalDevice.queueFamilies())
      .thenReturn(this.queueFamilies);
    when(this.physicalDevice.queueFamilyFindWithFlags((VulkanQueueFamilyPropertyFlag) any()))
      .thenAnswer(invocationOnMock -> {
        final var flags =
          (VulkanQueueFamilyPropertyFlag) invocationOnMock.getArguments()[0];
        for (final var queueFamily : this.queueFamilies.values()) {
          if (queueFamily.queueFlags().contains(flags)) {
            return Optional.of(queueFamily);
          }
        }
        return Optional.empty();
      });

    when(this.logicalDevice.queues())
      .thenReturn(this.queuesCreated);

    when(this.logicalDevice.findEnabledExtension("VK_KHR_surface", VulkanExtKHRSurfaceType.class))
      .thenReturn(Optional.of(this.surfaceExt));
    when(this.logicalDevice.findEnabledExtension("VK_KHR_swapchain", VulkanExtKHRSwapChainType.class))
      .thenReturn(Optional.of(this.swapchainExt));
    when(this.logicalDevice.createImageView(any()))
      .thenReturn(this.swapChainImageView);
    when(this.logicalDevice.createBinarySemaphore())
      .thenReturn(this.imageReadySemaphore)
      .thenReturn(this.imageRenderDoneSemaphore);
    when(this.logicalDevice.createFence(any()))
      .thenReturn(this.imageRenderDoneFence);
    when(this.logicalDevice.debugging())
      .thenReturn(this.debugging);

    this.strings =
      new RCStrings(Locale.ROOT);
    this.glfw =
      mock(RCGLFWFacadeType.class);
    this.window =
      new RCWindowWithSurface(
        this.strings,
        new RCWindow("X", 0L, this.glfw),
        this.surfaceExt,
        this.surface
      );

    this.executors =
      RCStandardExecutors.create(
        this.strings,
        new RCRendererID(0L)
      );

    this.window.configureForPhysicalDevice(this.physicalDevice);
  }

  /**
   * An implementation that provides a single queue that does everything.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueSingleMonolithic()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        1,
        Set.of(VulkanQueueFamilyPropertyFlag.values()),
        32,
        VulkanExtent3D.of(1, 1, 1)
      );

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties));

    this.queueFamilies.put(
      q0Properties.queueFamilyIndex(),
      q0Properties
    );

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );

    final var device =
      RCLogicalDevices.create(
        this.strings,
        this.executors,
        this.vulkanConfiguration,
        this.physicalDevice,
        this.window,
        VulkanPhysicalDeviceFeaturesFunctions.none(),
        new RCRendererID(0L)
      );

    final var expectedCreationInfo =
      VulkanLogicalDeviceCreateInfo.builder()
        .setQueueCreateInfos(List.of(
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
            .addQueuePriorities(1.0f)
            .build()
        ))
        .setFeatures(VulkanPhysicalDeviceFeaturesFunctions.none())
        .addEnabledExtensions("VK_KHR_swapchain")
        .build();

    assertEquals(expectedCreationInfo, this.deviceCreateInfoLogged);

    assertEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.computeQueue().queueFamilyIndex()
    );
    assertEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.transferQueue().queueFamilyIndex()
    );
  }

  /**
   * An implementation that provides a single graphics queue, and a separate
   * queue that does transfer and compute.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueGraphicsSeparateTransferCompute()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        1,
        Set.of(VK_QUEUE_GRAPHICS_BIT),
        32,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        1,
        Set.of(VK_QUEUE_COMPUTE_BIT),
        32,
        VulkanExtent3D.of(1, 1, 1)
      );

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties));

    this.queueFamilies.put(
      q0Properties.queueFamilyIndex(),
      q0Properties
    );
    this.queueFamilies.put(
      q1Properties.queueFamilyIndex(),
      q1Properties
    );

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(1))
    );

    final var device =
      RCLogicalDevices.create(
        this.strings,
        this.executors,
        this.vulkanConfiguration, this.physicalDevice,
        this.window,
        VulkanPhysicalDeviceFeaturesFunctions.none(),
        new RCRendererID(0L)
      );

    final var expectedCreationInfo =
      VulkanLogicalDeviceCreateInfo.builder()
        .setQueueCreateInfos(List.of(
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(1))
            .addQueuePriorities(1.0f)
            .build()
        ))
        .setFeatures(VulkanPhysicalDeviceFeaturesFunctions.none())
        .addEnabledExtensions("VK_KHR_swapchain")
        .build();

    assertEquals(expectedCreationInfo, this.deviceCreateInfoLogged);

    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.computeQueue().queueFamilyIndex()
    );
    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.transferQueue().queueFamilyIndex()
    );
  }

  /**
   * A demonstration of the queue setup for:
   *
   * <pre>
   * apiVersion    = 1.3.278 (4206870)
   * driverVersion = 24.1.2 (100667394)
   * vendorID      = 0x1002
   * deviceID      = 0x731f
   * deviceType    = PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
   * deviceName    = AMD Radeon RX 5700 (RADV NAVI10)
   * </pre>
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueAMDRadeonRX5700()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        1,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        4,
        Set.of(
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q2Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(2),
        1,
        Set.of(
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    /*
     * The first two queue families have present support on this hardware.
     */

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties, q1Properties));

    this.queueFamilies.put(
      q0Properties.queueFamilyIndex(),
      q0Properties
    );
    this.queueFamilies.put(
      q1Properties.queueFamilyIndex(),
      q1Properties
    );
    this.queueFamilies.put(
      q2Properties.queueFamilyIndex(),
      q2Properties
    );

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(1))
    );

    final var device =
      RCLogicalDevices.create(
        this.strings,
        this.executors,
        this.vulkanConfiguration,
        this.physicalDevice,
        this.window,
        VulkanPhysicalDeviceFeaturesFunctions.none(),
        new RCRendererID(0L)
      );

    final var expectedCreationInfo =
      VulkanLogicalDeviceCreateInfo.builder()
        .setQueueCreateInfos(List.of(
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(1))
            .addQueuePriorities(1.0f)
            .addQueuePriorities(1.0f)
            .build()
        ))
        .setFeatures(VulkanPhysicalDeviceFeaturesFunctions.none())
        .addEnabledExtensions("VK_KHR_swapchain")
        .build();

    assertEquals(expectedCreationInfo, this.deviceCreateInfoLogged);

    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.computeQueue().queueFamilyIndex()
    );
    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.transferQueue().queueFamilyIndex()
    );
  }

  /**
   * A demonstration of the queue setup for:
   *
   * <pre>
   * apiVersion    = 1.3.260 (4206852)
   * driverVersion = 546.33.0.0 (2290630656)
   * vendorID      = 0x10de
   * deviceID      = 0x2684
   * deviceType    = PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
   * deviceName    = NVIDIA GeForce RTX 4090
   * </pre>
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueNVIDIA4090()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        16,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        2,
        Set.of(
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q2Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(2),
        8,
        Set.of(
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q3Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(3),
        1,
        Set.of(
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        32,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);
    this.queueFamilies.put(q1Properties.queueFamilyIndex(), q1Properties);
    this.queueFamilies.put(q2Properties.queueFamilyIndex(), q2Properties);
    this.queueFamilies.put(q3Properties.queueFamilyIndex(), q3Properties);

    /*
     * The first and third queue families have present support on this hardware.
     */

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties, q2Properties));

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(1))
    );
    this.queuesCreated.add(
      fakeQueue(q2Properties, new VulkanQueueIndex(2))
    );

    final var device =
      RCLogicalDevices.create(
        this.strings,
        this.executors,
        this.vulkanConfiguration,
        this.physicalDevice,
        this.window,
        VulkanPhysicalDeviceFeaturesFunctions.none(),
        new RCRendererID(0L)
      );

    final var expectedCreationInfo =
      VulkanLogicalDeviceCreateInfo.builder()
        .setQueueCreateInfos(List.of(
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(1))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(2))
            .addQueuePriorities(1.0f)
            .build()
        ))
        .setFeatures(VulkanPhysicalDeviceFeaturesFunctions.none())
        .addEnabledExtensions("VK_KHR_swapchain")
        .build();

    assertEquals(expectedCreationInfo, this.deviceCreateInfoLogged);

    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.computeQueue().queueFamilyIndex()
    );
    assertNotEquals(
      device.graphicsQueue().queueFamilyIndex(),
      device.transferQueue().queueFamilyIndex()
    );
  }

  /**
   * Without presentation support, logical device creation fails if it is
   * required.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueErrorNoPresentation()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        16,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of());

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );

    final var ex =
      assertThrows(RCVulkanException.class, () -> {
        RCLogicalDevices.create(
          this.strings,
          this.executors,
          this.vulkanConfiguration,
          this.physicalDevice,
          this.window,
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          new RCRendererID(0L)
        );
      });

    assertEquals(VULKAN_QUEUE_MISSING.codeName(), ex.errorCode());
  }

  /**
   * An example of a weird device where there's an entirely separate queue
   * family that only does presentation, and no other queue families do. The
   * other queue families are also deliberately very specialized so that we
   * end up choosing one queue from each family.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueWeird()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        1,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        1,
        Set.of(
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q2Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(2),
        1,
        Set.of(
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q3Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(3),
        1,
        Set.of(VK_QUEUE_TRANSFER_BIT),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);
    this.queueFamilies.put(q1Properties.queueFamilyIndex(), q1Properties);
    this.queueFamilies.put(q2Properties.queueFamilyIndex(), q2Properties);
    this.queueFamilies.put(q3Properties.queueFamilyIndex(), q3Properties);

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q3Properties));

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(1))
    );
    this.queuesCreated.add(
      fakeQueue(q2Properties, new VulkanQueueIndex(2))
    );
    this.queuesCreated.add(
      fakeQueue(q3Properties, new VulkanQueueIndex(3))
    );

    final var device =
      RCLogicalDevices.create(
        this.strings,
        this.executors,
        this.vulkanConfiguration,
        this.physicalDevice,
        this.window,
        VulkanPhysicalDeviceFeaturesFunctions.none(),
        new RCRendererID(0L)
      );

    final var expectedCreationInfo =
      VulkanLogicalDeviceCreateInfo.builder()
        .setQueueCreateInfos(List.of(
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(0))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(1))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(2))
            .addQueuePriorities(1.0f)
            .build(),
          VulkanLogicalDeviceQueueCreateInfo.builder()
            .setQueueFamilyIndex(new VulkanQueueFamilyIndex(3))
            .addQueuePriorities(1.0f)
            .build()
        ))
        .setFeatures(VulkanPhysicalDeviceFeaturesFunctions.none())
        .addEnabledExtensions("VK_KHR_swapchain")
        .build();

    assertEquals(expectedCreationInfo, this.deviceCreateInfoLogged);
  }

  /**
   * We fail with an illegal state exception if the device implementation
   * (in jcoronado) fails to return the right queues.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueFailedReturns0()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        3,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties));

    final var ex =
      assertThrows(IllegalStateException.class, () -> {
        RCLogicalDevices.create(
          this.strings,
          this.executors,
          this.vulkanConfiguration,
          this.physicalDevice,
          this.window,
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          new RCRendererID(0L)
        );
      });
    assertTrue(ex.getMessage().contains("graphics queue"));
  }

  /**
   * We fail with an illegal state exception if the device implementation
   * (in jcoronado) fails to return the right queues.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueFailedReturns1()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        2,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        1,
        Set.of(
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_TRANSFER_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);
    this.queueFamilies.put(q1Properties.queueFamilyIndex(), q1Properties);

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties));

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );

    final var ex =
      assertThrows(IllegalStateException.class, () -> {
        RCLogicalDevices.create(
          this.strings,
          this.executors,
          this.vulkanConfiguration,
          this.physicalDevice,
          this.window,
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          new RCRendererID(0L)
        );
      });
    assertTrue(ex.getMessage().contains("transfer queue"));
  }

  /**
   * We fail with an illegal state exception if the device implementation
   * (in jcoronado) fails to return the right queues.
   *
   * @throws Exception On errors
   */

  @Test
  public void testQueueFailedReturns2()
    throws Exception
  {
    final var q0Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(0),
        1,
        Set.of(
          VK_QUEUE_GRAPHICS_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q1Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(1),
        1,
        Set.of(
          VK_QUEUE_TRANSFER_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    final var q2Properties =
      VulkanQueueFamilyProperties.of(
        new VulkanQueueFamilyIndex(2),
        1,
        Set.of(
          VK_QUEUE_COMPUTE_BIT,
          VK_QUEUE_SPARSE_BINDING_BIT
        ),
        64,
        VulkanExtent3D.of(1, 1, 1)
      );

    this.queueFamilies.put(q0Properties.queueFamilyIndex(), q0Properties);
    this.queueFamilies.put(q1Properties.queueFamilyIndex(), q1Properties);
    this.queueFamilies.put(q2Properties.queueFamilyIndex(), q2Properties);

    when(this.surfaceExt.surfaceSupport(any(), any()))
      .thenReturn(List.of(q0Properties));

    this.queuesCreated.add(
      fakeQueue(q0Properties, new VulkanQueueIndex(0))
    );
    this.queuesCreated.add(
      fakeQueue(q1Properties, new VulkanQueueIndex(0))
    );

    final var ex =
      assertThrows(IllegalStateException.class, () -> {
        RCLogicalDevices.create(
          this.strings,
          this.executors,
          this.vulkanConfiguration,
          this.physicalDevice,
          this.window,
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          new RCRendererID(0L)
        );
      });
    assertTrue(ex.getMessage().contains("compute queue"));
  }

  private static VulkanQueueType fakeQueue(
    final VulkanQueueFamilyProperties properties,
    final VulkanQueueIndex index)
  {
    final var queueResult =
      mock(VulkanQueueType.class);
    when(queueResult.queueFamilyIndex())
      .thenReturn(properties.queueFamilyIndex());
    when(queueResult.queueIndex())
      .thenReturn(index);
    when(queueResult.queueFamilyProperties())
      .thenReturn(properties);
    return queueResult;
  }
}
