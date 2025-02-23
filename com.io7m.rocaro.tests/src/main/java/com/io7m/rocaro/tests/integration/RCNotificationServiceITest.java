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

import com.io7m.jcoronado.api.VulkanCommandBufferSubmitInfo;
import com.io7m.jcoronado.api.VulkanCommandPoolCreateInfo;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanFenceCreateInfo;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSemaphoreSubmitInfo;
import com.io7m.jcoronado.api.VulkanSemaphoreTimelineType;
import com.io7m.jcoronado.api.VulkanSemaphoreTimelineWait;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
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
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.vanilla.internal.notifications.RCNotificationServiceType;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutorType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static com.io7m.jcoronado.api.VulkanCommandBufferUsageFlag.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

@Tag("Real-Vulkan-Integration")
public final class RCNotificationServiceITest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCNotificationServiceITest.class);

  private RendererType renderer;
  private RCNotificationServiceType notifications;
  private RCVulkanRendererType vulkanRenderer;
  private RCDeviceType device;
  private VulkanLogicalDeviceType vulkanDevice;

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
        "RCNotificationServiceITest",
        Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(VulkanLWJGLInstanceProvider.create())
        .setVmaAllocators(VMALWJGLAllocatorProvider.create())
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
    this.vulkanRenderer =
      this.renderer.requireService(RCVulkanRendererType.class);
    this.notifications =
      this.renderer.requireService(RCNotificationServiceType.class);
    this.device =
      this.vulkanRenderer.device();
    this.vulkanDevice =
      this.device.device();
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
  public void testFenceSignalledTransfer()
    throws Exception
  {
    this.executeFenceSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.transferQueue(),
      this.notifications::registerFence
    );
  }

  @Test
  public void testFenceSignalledCompute()
    throws Exception
  {
    this.executeFenceSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.computeQueue(),
      this.notifications::registerFence
    );
  }

  @Test
  public void testFenceSignalledGraphics()
    throws Exception
  {
    this.executeFenceSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.graphicsQueue(),
      this.notifications::registerFence
    );
  }

  @Test
  public void testTimelineSemaphoreSignalledTransfer()
    throws Exception
  {
    this.executeTimelineSemaphoreSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.transferQueue(),
      this.notifications::registerTimelineSemaphore
    );
  }

  @Test
  public void testTimelineSemaphoreSignalledCompute()
    throws Exception
  {
    this.executeTimelineSemaphoreSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.computeQueue(),
      this.notifications::registerTimelineSemaphore
    );
  }

  @Test
  public void testTimelineSemaphoreSignalledGraphics()
    throws Exception
  {
    this.executeTimelineSemaphoreSignalled(
      (RCExecutorType) this.device.gpuExecutor(),
      this.device.graphicsQueue(),
      this.notifications::registerTimelineSemaphore
    );
  }


  private void executeFenceSignalled(
    final RCExecutorType executor,
    final VulkanQueueType queue,
    final Function<VulkanFenceType, CompletableFuture<?>> register)
    throws Exception
  {
    final var commandPool =
      this.device.registerResource(
        executor.executeAndWait(() -> {
          LOG.debug("Creating command pool…");
          return this.vulkanDevice.createCommandPool(
            VulkanCommandPoolCreateInfo.builder()
              .setQueueFamilyIndex(queue.queueFamilyIndex())
              .build()
          );
        })
      );

    this.vulkanDevice.debugging()
      .setObjectName(commandPool, "CommandPool[FenceTest]");

    final var fence =
      this.device.registerResource(
        executor.executeAndWait(() -> {
          LOG.debug("Creating fence…");
          return this.vulkanDevice.createFence(
            VulkanFenceCreateInfo.builder()
              .build()
          );
        })
      );

    this.vulkanDevice.debugging()
      .setObjectName(commandPool, "Fence[Test]");

    LOG.debug("Registering fence…");
    final var fenceFuture =
      register.apply(fence);

    executor.execute(() -> {
      try {
        LOG.debug("Recording command buffer…");
        final var cmd =
          this.vulkanDevice.createCommandBuffer(
            commandPool,
            VK_COMMAND_BUFFER_LEVEL_PRIMARY
          );

        this.vulkanDevice.debugging()
          .setObjectName(cmd, "CommandBuffer[FenceTest]");

        this.device.registerResource(cmd);
        cmd.beginCommandBuffer(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        cmd.endCommandBuffer();

        LOG.debug("Submitting command buffer…");
        queue.submit(
          List.of(
            VulkanSubmitInfo.builder()
              .addCommandBuffers(
                VulkanCommandBufferSubmitInfo.builder()
                  .setCommandBuffer(cmd)
                  .build()
              )
              .build()
          ),
          Optional.of(fence)
        );
      } catch (final VulkanException e) {
        throw new RuntimeException(e);
      }
    });

    LOG.debug("Waiting for fence…");
    fenceFuture.get(5L, TimeUnit.SECONDS);
  }

  private void executeTimelineSemaphoreSignalled(
    final RCExecutorType executor,
    final VulkanQueueType queue,
    final Function<VulkanSemaphoreTimelineWait, CompletableFuture<?>> register)
    throws Exception
  {
    final var commandPool =
      this.device.registerResource(
        executor.executeAndWait(() -> {
          LOG.debug("Creating command pool…");
          return this.vulkanDevice.createCommandPool(
            VulkanCommandPoolCreateInfo.builder()
              .setQueueFamilyIndex(queue.queueFamilyIndex())
              .build()
          );
        })
      );

    this.vulkanDevice.debugging()
      .setObjectName(commandPool, "CommandPool[TimelineSemaphoreTest]");

    final var semaphore =
      this.device.registerResource(
        executor.executeAndWait(() -> {
          LOG.debug("Creating semaphore…");
          return this.vulkanDevice.createTimelineSemaphore(10L);
        })
      );

    this.vulkanDevice.debugging()
      .setObjectName(commandPool, "TimelineSemaphore[Test]");

    LOG.debug("Registering timeline semaphore…");
    final var semaphoreFuture =
      register.apply(new VulkanSemaphoreTimelineWait(semaphore, 20L));

    executor.execute(() -> {
      try {
        LOG.debug("Recording command buffer…");
        final var cmd =
          this.vulkanDevice.createCommandBuffer(
            commandPool,
            VK_COMMAND_BUFFER_LEVEL_PRIMARY
          );

        this.vulkanDevice.debugging()
          .setObjectName(cmd, "CommandBuffer[TimelineSemaphoreTest]");

        this.device.registerResource(cmd);
        cmd.beginCommandBuffer(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        cmd.endCommandBuffer();

        LOG.debug("Submitting command buffer…");
        queue.submit(
          List.of(
            VulkanSubmitInfo.builder()
              .addCommandBuffers(
                VulkanCommandBufferSubmitInfo.builder()
                  .setCommandBuffer(cmd)
                  .build()
              )
              .addSignalSemaphores(
                VulkanSemaphoreSubmitInfo.builder()
                  .setSemaphore(semaphore)
                  .setValue(20L)
                  .build()
              )
              .build()
          ),
          Optional.empty()
        );
      } catch (final VulkanException e) {
        throw new RuntimeException(e);
      }
    });

    LOG.debug("Waiting for semaphore…");
    semaphoreFuture.get(5L, TimeUnit.SECONDS);
  }
}
