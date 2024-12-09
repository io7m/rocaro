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


package com.io7m.rocaro.tests.integration;

import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPractices;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesAMD;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesNVIDIA;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateCore;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateSync;
import com.io7m.jcoronado.lwjgl.VMALWJGLAllocatorProvider;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.api.transfers.RCTransferImageColorBasic;
import com.io7m.rocaro.api.transfers.RCTransferServiceType;
import com.io7m.rocaro.vanilla.internal.fences.RCFenceServiceType;
import com.io7m.rocaro.vanilla.internal.images.RCImageColorBasic;
import com.io7m.rocaro.vanilla.internal.renderdoc.RCRenderDocServiceType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

@Tag("Real-Vulkan-Integration")
public final class RCTransferServiceITest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCTransferServiceITest.class);

  private RendererType renderer;
  private RCFenceServiceType fences;
  private RCVulkanRendererType vulkanRenderer;
  private RCDeviceType device;
  private VulkanLogicalDeviceType vulkanDevice;
  private RCTransferServiceType transfers;
  private RCRenderDocServiceType renderdoc;

  @BeforeEach
  public void setup()
    throws RocaroException
  {
    final var renderers =
      ServiceLoader.load(RendererFactoryType.class)
        .findFirst()
        .orElseThrow();

    final var builder = renderers.builder();
    builder.setDisplaySelection(
      new RCDisplaySelectionWindowed(
        "RCFenceServiceITest",
        Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(VulkanLWJGLInstanceProvider.create())
        .setVmaAllocators(VMALWJGLAllocatorProvider.create())
        .setEnableRenderDocSupport(true)
        .addEnableValidation(
          new VulkanValidationValidateCore(true))
        .addEnableValidation(
          new VulkanValidationValidateSync(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPractices(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPracticesAMD(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPracticesNVIDIA(true))
        .build()
    );

    this.renderer =
      builder.start();
    this.renderdoc =
      this.renderer.requireService(RCRenderDocServiceType.class);
    this.vulkanRenderer =
      this.renderer.requireService(RCVulkanRendererType.class);
    this.transfers =
      this.renderer.requireService(RCTransferServiceType.class);
    this.device =
      this.vulkanRenderer.device();
    this.vulkanDevice =
      this.device.device();

    this.renderdoc.triggerCapture();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.renderer.close();

    LOG.debug("Waiting for window system to settle…");
    Thread.sleep(1_000L);
  }

  @Test
  public void testTransferImageMultiQueue()
    throws Exception
  {
    Assumptions.assumeTrue(
      !Objects.equals(this.device.transferQueue(), this.device.graphicsQueue()),
      "Device exposes different transfer and graphics queues."
    );

    final var future =
      this.transfers.transfer(
        RCTransferImageColorBasic.builder()
          .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
          .setFormat(VulkanFormat.VK_FORMAT_R8_UNORM)
          .setSize(Vector2I.of(8, 8))
          .setTargetQueue(RCDeviceQueueCategory.GRAPHICS)
          .setName("Example")
          .setDataCopier(target -> {
            target.fill((byte) 0x7f);
          })
          .build()
      );

    this.executeWaitingFrames();

    LOG.debug("Waiting for transfer...");
    final var r = (RCImageColorBasic)
      future.get(5L, TimeUnit.SECONDS);
    LOG.debug("Transferred: {}", r);

    this.device.registerResource(r.data());
    this.device.registerResource(r.view());
  }

  private void executeWaitingFrames()
    throws RocaroException, InterruptedException
  {
    /*
     * Execute a few frames to give the GPU time to complete the work and
     * to allow the status of the fence to be checked.
     */

    for (int index = 0; index < 10; ++index) {
      this.renderer.executeFrame(c -> c.executeGraph("Empty"));
      Thread.sleep(100L);
    }
  }

  @Test
  public void testTransferImageSingleQueue()
    throws Exception
  {
    final var future =
      this.transfers.transfer(
        RCTransferImageColorBasic.builder()
          .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
          .setFormat(VulkanFormat.VK_FORMAT_R8_UNORM)
          .setSize(Vector2I.of(8, 8))
          .setTargetQueue(RCDeviceQueueCategory.TRANSFER)
          .setName("Example")
          .setDataCopier(target -> {
            target.fill((byte) 0x7f);
          })
          .build()
      );

    this.executeWaitingFrames();

    LOG.debug("Waiting for transfer...");
    final var r = (RCImageColorBasic)
      future.get(5L, TimeUnit.SECONDS);
    LOG.debug("Transferred: {}", r);

    this.device.registerResource(r.data());
    this.device.registerResource(r.view());
  }
}
