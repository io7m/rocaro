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


package com.io7m.rocaro.vanilla.internal.transfers;

import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanCommandPoolCreateInfo;
import com.io7m.jcoronado.api.VulkanCommandPoolType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.transfers.RCTransferImageColorBasicType;
import com.io7m.rocaro.api.transfers.RCTransferJFREventExecuted;
import com.io7m.rocaro.api.transfers.RCTransferOperationType;
import com.io7m.rocaro.api.transfers.RCTransferServiceType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.fences.RCFenceServiceType;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutors;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static com.io7m.rocaro.api.RCUnit.UNIT;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU_TRANSFER;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.TRANSFER_IO;

/**
 * The transfer service.
 */

public final class RCTransferService
  extends RCObject
  implements RCTransferServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCTransferService.class);

  private final RCFenceServiceType fences;
  private final RCDeviceType device;
  private final VMAAllocatorType allocator;
  private final ExecutorService taskExecutor;

  @RCThread(GPU_TRANSFER)
  private final VulkanCommandPoolType transferCommandPool;
  private final VulkanCommandPoolType graphicsCommandPool;
  private final VulkanCommandPoolType computeCommandPool;
  private final RCStrings strings;
  private final CloseableCollectionType<RocaroException> resources;

  private RCTransferService(
    final CloseableCollectionType<RocaroException> inResources,
    final RCStrings inStrings,
    final RCFenceServiceType inFences,
    final RCDeviceType inDevice,
    final VMAAllocatorType inAllocator,
    final ExecutorService inTaskExecutor,
    final VulkanCommandPoolType inTransferCommandPool,
    final VulkanCommandPoolType inGraphicsCommandPool,
    final VulkanCommandPoolType inComputeCommandPool)
  {
    this.resources =
      Objects.requireNonNull(inResources, "inResources");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.fences =
      Objects.requireNonNull(inFences, "fences");
    this.device =
      Objects.requireNonNull(inDevice, "device");
    this.allocator =
      Objects.requireNonNull(inAllocator, "inAllocator");
    this.taskExecutor =
      Objects.requireNonNull(inTaskExecutor, "taskExecutor");
    this.transferCommandPool =
      Objects.requireNonNull(inTransferCommandPool, "transferCommandPool");
    this.graphicsCommandPool =
      Objects.requireNonNull(inGraphicsCommandPool, "graphicsCommandPool");
    this.computeCommandPool =
      Objects.requireNonNull(inComputeCommandPool, "computeCommandPool");
  }

  /**
   * Create a transfer service.
   *
   * @param services The service directory
   *
   * @return The service
   *
   * @throws RocaroException On errors
   */

  public static RCTransferService create(
    final RPServiceDirectoryType services)
    throws RocaroException
  {
    final var strings =
      services.requireService(RCStrings.class);
    final var renderer =
      services.requireService(RCVulkanRendererType.class);
    final var fences =
      services.requireService(RCFenceServiceType.class);

    final var resources =
      RCResourceCollections.create(strings);

    try {
      final var device =
        renderer.device();
      final var allocator =
        device.allocator();
      final var debugging =
        device.device().debugging();

      final var taskExecutor =
        resources.add(
          RCExecutors.createVirtualExecutor(
            "transfer-service-task",
            renderer.id(),
            TRANSFER_IO
          )
        );

      final var transferCommandPool =
        device.registerTransferResource(
          RCExecutors.executeAndWait(
            device.transferExecutor(),
            () -> {
              final var poolInfo =
                VulkanCommandPoolCreateInfo.builder()
                  .setQueueFamilyIndex(device.transferQueue().queueFamilyIndex())
                  .build();

              final var commandPool =
                device.device().createCommandPool(poolInfo);

              debugging.setObjectName(
                commandPool,
                "TransferCommandPool[Transfer]");
              return commandPool;
            }
          )
        );

      final var graphicsCommandPool =
        device.registerGraphicsResource(
          RCExecutors.executeAndWait(
            device.graphicsExecutor(),
            () -> {
              final var poolInfo =
                VulkanCommandPoolCreateInfo.builder()
                  .setQueueFamilyIndex(device.graphicsQueue().queueFamilyIndex())
                  .build();

              final var commandPool =
                device.device().createCommandPool(poolInfo);

              debugging.setObjectName(
                commandPool,
                "TransferCommandPool[Graphics]");
              return commandPool;
            }
          )
        );

      final var computeCommandPool =
        device.registerComputeResource(
          RCExecutors.executeAndWait(
            device.graphicsExecutor(),
            () -> {
              final var poolInfo =
                VulkanCommandPoolCreateInfo.builder()
                  .setQueueFamilyIndex(device.computeQueue().queueFamilyIndex())
                  .build();

              final var commandPool =
                device.device().createCommandPool(poolInfo);

              debugging.setObjectName(
                commandPool,
                "TransferCommandPool[Compute]");
              return commandPool;
            }
          )
        );

      return new RCTransferService(
        resources,
        strings,
        fences,
        device,
        allocator,
        taskExecutor,
        transferCommandPool,
        graphicsCommandPool,
        computeCommandPool
      );

    } catch (final Throwable e) {
      resources.close();
      throw e;
    }
  }

  private <T> CompletableFuture<T> executeOp(
    final RCTransferOperationType<T> operation,
    final RCTransferTaskType<T> task)
  {
    final var future = new CompletableFuture<T>();
    this.taskExecutor.execute(() -> {
      final var ev = new RCTransferJFREventExecuted();
      ev.transferID = operation.id().toString();
      ev.type = operation.getClass().getSimpleName();

      try {
        try (final var _ = task) {
          future.complete(task.execute());
        }
        ev.message = "Transfer completed";
      } catch (final Throwable e) {
        ev.message = "Transfer failed (%s)".formatted(e.getMessage());
        future.completeExceptionally(e);
      } finally {
        if (ev.shouldCommit()) {
          ev.commit();
        }
      }
    });
    return future;
  }

  @Override
  public <T> CompletableFuture<T> transfer(
    final RCTransferOperationType<T> operation)
  {
    return switch (operation) {
      case final RCTransferImageColorBasicType image -> {
        yield (CompletableFuture<T>) this.executeOp(
          image,
          new RCTransferImageColorBasicTask(
            this.device,
            this.allocator,
            this::createCommandBuffer,
            this.strings,
            this.fences,
            image
          )
        );
      }
    };
  }

  private VulkanCommandBufferType createCommandBuffer(
    final CloseableCollectionType<RocaroException> taskResources,
    final VulkanQueueType queue,
    final String name)
    throws RocaroException
  {
    return switch (this.device.categoryForQueue(queue)) {
      case COMPUTE -> {
        final var executor = this.device.computeExecutor();
        yield RCExecutors.executeAndWait(
          executor,
          () -> {
            final var commandBuffer =
              this.device.device()
                .createCommandBuffer(
                  this.computeCommandPool,
                  VK_COMMAND_BUFFER_LEVEL_PRIMARY
                );

            this.device.device()
              .debugging()
              .setObjectName(commandBuffer, name);

            taskResources.add(() -> {
              RCExecutors.executeAndWait(executor, () -> {
                commandBuffer.close();
                return UNIT;
              });
            });
            return commandBuffer;
          });
      }

      case GRAPHICS -> {
        final var executor = this.device.graphicsExecutor();
        yield RCExecutors.executeAndWait(
          executor,
          () -> {
            final var commandBuffer =
              this.device.device()
                .createCommandBuffer(
                  this.graphicsCommandPool,
                  VK_COMMAND_BUFFER_LEVEL_PRIMARY
                );

            this.device.device()
              .debugging()
              .setObjectName(commandBuffer, name);

            taskResources.add(() -> {
              RCExecutors.executeAndWait(executor, () -> {
                commandBuffer.close();
                return UNIT;
              });
            });
            return commandBuffer;
          });
      }

      case TRANSFER -> {
        final var executor = this.device.transferExecutor();
        yield RCExecutors.executeAndWait(
          executor,
          () -> {
            final var commandBuffer =
              this.device.device()
                .createCommandBuffer(
                  this.transferCommandPool,
                  VK_COMMAND_BUFFER_LEVEL_PRIMARY
                );

            this.device.device()
              .debugging()
              .setObjectName(commandBuffer, name);

            taskResources.add(() -> {
              RCExecutors.executeAndWait(executor, () -> {
                commandBuffer.close();
                return UNIT;
              });
            });
            return commandBuffer;
          });
      }
    };
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
    this.resources.close();
  }

  @Override
  public String description()
  {
    return "Transfer service.";
  }
}
