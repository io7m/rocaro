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

import com.io7m.jcoronado.api.VulkanQueueIndex;
import com.io7m.jcoronado.fake.VFakeExtKHRSurface;
import com.io7m.jcoronado.fake.VFakeExtKHRSwapChain;
import com.io7m.jcoronado.fake.VFakeInstance;
import com.io7m.jcoronado.fake.VFakeInstances;
import com.io7m.jcoronado.fake.VFakeLogicalDevice;
import com.io7m.jcoronado.fake.VFakeQueue;
import com.io7m.jcoronado.vma.VMAAllocatorProviderType;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3I;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.displays.RCDisplay;
import com.io7m.rocaro.api.displays.RCDisplayException;
import com.io7m.rocaro.api.displays.RCDisplayMode;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCVersions;
import com.io7m.rocaro.vanilla.internal.RendererBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DISPLAY_NONE_SUITABLE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DISPLAY_WINDOW_CREATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class RendererBuilderTest
{
  private static final RCStrings STRINGS =
    new RCStrings(Locale.ROOT);
  private static final RCVersions VERSIONS =
    new RCVersions(Locale.ROOT);

  private static final RCDisplayMode VGA_60HZ =
    new RCDisplayMode(
      Vector2I.of(640, 480),
      Vector3I.of(8, 8, 8),
      60.0
    );
  private static final RCDisplay BASIC_DISPLAY_PRIMARY =
    new RCDisplay(
      true,
      100L,
      "Display 0",
      Vector2D.of(100, 100),
      List.of(VGA_60HZ)
    );

  private RCGLFWFacadeType glfw;
  private VFakeInstances instances;
  private VMAAllocatorProviderType vmaProvider;
  private VMAAllocatorType vma;
  private RendererVulkanConfiguration vulkanConfiguration;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.glfw =
      Mockito.mock(RCGLFWFacadeType.class);
    this.vmaProvider =
      Mockito.mock(VMAAllocatorProviderType.class);
    this.vma =
      Mockito.mock(VMAAllocatorType.class);

    Mockito.when(this.vmaProvider.createAllocator(Mockito.any()))
      .thenReturn(this.vma);

    this.instances =
      RCFakeVulkan.fake();

    final var extensionProperties =
      Map.ofEntries(
        Map.entry(
          VFakeExtKHRSurface.properties().name(),
          VFakeExtKHRSurface.properties()
        ),
        Map.entry(
          VFakeExtKHRSwapChain.properties().name(),
          VFakeExtKHRSwapChain.properties()
        )
      );

    this.instances.setExtensions(extensionProperties);

    final var extSurface =
      new VFakeExtKHRSurface();
    final var extSwapChain =
      new VFakeExtKHRSwapChain();

    final var extensions =
      Map.ofEntries(
        Map.entry(VFakeExtKHRSurface.properties().name(), extSurface),
        Map.entry(VFakeExtKHRSwapChain.properties().name(), extSwapChain)
      );

    final var instance =
      new VFakeInstance(this.instances, extensions);

    this.instances.setNextInstance(instance);

    final var fakePhysicalDevice =
      instance.getPhysicalDevices()
        .get(0);

    fakePhysicalDevice.setExtensions(extensionProperties);

    extSurface.setSurfaceSupport(
      List.of(fakePhysicalDevice.queueFamilies().firstEntry().getValue())
    );

    final var fakeLogicalDevice =
      new VFakeLogicalDevice(fakePhysicalDevice);

    fakePhysicalDevice.setNextDevice(fakeLogicalDevice);

    fakeLogicalDevice.setQueues(
      List.of(
        new VFakeQueue(
          fakeLogicalDevice,
          fakePhysicalDevice.queueFamilies().firstEntry().getValue(),
          new VulkanQueueIndex(0)
        )
      )
    );

    fakeLogicalDevice.setEnabledExtensions(extensions);

    this.vulkanConfiguration =
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(this.instances)
        .setVmaAllocators(this.vmaProvider)
        .build();
  }

  /**
   * Opening a renderer fails if no suitable display can be found.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNoSuitableDisplay()
    throws Exception
  {
    when(this.glfw.displays())
      .thenReturn(List.of());

    final var b =
      new RendererBuilder(Locale.ROOT, STRINGS, VERSIONS, this.glfw);
    b.setVulkanConfiguration(this.vulkanConfiguration);

    final var ex =
      assertThrows(RCDisplayException.class, b::start);

    assertEquals(
      DISPLAY_NONE_SUITABLE.codeName(),
      ex.errorCode()
    );
  }

  /**
   * Opening a renderer fails if the underlying window system reports a
   * failure.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateFullscreenFails()
    throws Exception
  {
    when(this.glfw.displays())
      .thenReturn(List.of(BASIC_DISPLAY_PRIMARY));
    when(this.glfw.windowCreateFullscreen(anyInt(), anyInt(), any(), anyLong()))
      .thenReturn(0L);

    final var b =
      new RendererBuilder(Locale.ROOT, STRINGS, VERSIONS, this.glfw);
    b.setVulkanConfiguration(this.vulkanConfiguration);

    final var ex =
      assertThrows(RCDisplayException.class, b::start);

    assertEquals(
      DISPLAY_WINDOW_CREATION.codeName(),
      ex.errorCode()
    );

    verify(this.glfw, new Times(1))
      .windowCreateFullscreen(
        eq(640),
        eq(480),
        eq("Rocaro"),
        eq(BASIC_DISPLAY_PRIMARY.id())
      );
  }

  /**
   * Opening a renderer fails if the underlying window system reports a
   * failure.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateWindowFails()
    throws Exception
  {
    when(this.glfw.displays())
      .thenReturn(List.of(BASIC_DISPLAY_PRIMARY));
    when(this.glfw.windowCreateWindowed(anyInt(), anyInt(), any()))
      .thenReturn(0L);

    final var b =
      new RendererBuilder(Locale.ROOT, STRINGS, VERSIONS, this.glfw);
    b.setVulkanConfiguration(this.vulkanConfiguration);

    b.setDisplaySelection(
      new RCDisplaySelectionWindowed("X", Vector2I.of(640, 480))
    );

    final var ex =
      assertThrows(RCDisplayException.class, b::start);

    assertEquals(
      DISPLAY_WINDOW_CREATION.codeName(),
      ex.errorCode()
    );

    verify(this.glfw, new Times(1))
      .windowCreateWindowed(
        eq(640),
        eq(480),
        eq("X")
      );
  }

  /**
   * Opening a renderer succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateFullscreenOK()
    throws Exception
  {
    when(this.glfw.displays())
      .thenReturn(List.of(BASIC_DISPLAY_PRIMARY));
    when(this.glfw.windowCreateFullscreen(anyInt(), anyInt(), any(), anyLong()))
      .thenReturn(0x30405060L);

    final var b =
      new RendererBuilder(Locale.ROOT, STRINGS, VERSIONS, this.glfw);
    b.setVulkanConfiguration(this.vulkanConfiguration);

    try (final var r = b.start()) {

    }

    verify(this.glfw, new Times(1))
      .windowCreateFullscreen(
        eq(640),
        eq(480),
        eq("Rocaro"),
        eq(BASIC_DISPLAY_PRIMARY.id())
      );
  }

  /**
   * Opening a renderer succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreateWindowedOK()
    throws Exception
  {
    when(this.glfw.displays())
      .thenReturn(List.of(BASIC_DISPLAY_PRIMARY));
    when(this.glfw.windowCreateWindowed(anyInt(), anyInt(), any()))
      .thenReturn(0x30405060L);

    final var b =
      new RendererBuilder(Locale.ROOT, STRINGS, VERSIONS, this.glfw);
    b.setVulkanConfiguration(this.vulkanConfiguration);

    b.setDisplaySelection(
      new RCDisplaySelectionWindowed("X", Vector2I.of(640, 480))
    );

    try (final var r = b.start()) {

    }

    verify(this.glfw, new Times(1))
      .windowCreateWindowed(
        eq(640),
        eq(480),
        eq("X")
      );
  }
}
