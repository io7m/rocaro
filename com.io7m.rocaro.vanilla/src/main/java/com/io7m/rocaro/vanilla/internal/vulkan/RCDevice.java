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

import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_COMPUTE_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.rocaro.api.RCUnit.UNIT;

/**
 * A device.
 */

public final class RCDevice
  extends RCObject
  implements RCDeviceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCDevice.class);

  private final VulkanLogicalDeviceType device;
  private final VMAAllocatorType allocator;
  private final VulkanQueueType graphicsQueue;
  private final ExecutorService graphicsExecutor;
  private final VulkanQueueType transferQueue;
  private final ExecutorService transferExecutor;
  private final VulkanQueueType computeQueue;
  private final ExecutorService computeExecutor;
  private final CloseableCollectionType<RocaroException> graphicsResources;
  private final CloseableCollectionType<RocaroException> transferResources;
  private final CloseableCollectionType<RocaroException> computeResources;
  private final CloseableCollectionType<RocaroException> allResources;

  RCDevice(
    final RCStrings strings,
    final VulkanLogicalDeviceType inDevice,
    final VMAAllocatorType inAllocator,
    final VulkanQueueType inGraphicsQueue,
    final ExecutorService inGraphicsExecutor,
    final VulkanQueueType inTransferQueue,
    final ExecutorService inTransferExecutor,
    final VulkanQueueType inComputeQueue,
    final ExecutorService inComputeExecutor)
  {
    this.device =
      Objects.requireNonNull(inDevice, "device");
    this.allocator =
      Objects.requireNonNull(inAllocator, "allocator");
    this.graphicsQueue =
      Objects.requireNonNull(inGraphicsQueue, "graphicsQueue");
    this.graphicsExecutor =
      Objects.requireNonNull(inGraphicsExecutor, "graphicsExecutor");
    this.transferQueue =
      Objects.requireNonNull(inTransferQueue, "transferQueue");
    this.transferExecutor =
      Objects.requireNonNull(inTransferExecutor, "transferExecutor");
    this.computeQueue =
      Objects.requireNonNull(inComputeQueue, "computeQueue");
    this.computeExecutor =
      Objects.requireNonNull(inComputeExecutor, "computeExecutor");

    queueCheck(inGraphicsQueue, VK_QUEUE_GRAPHICS_BIT);
    queueCheck(inComputeQueue, VK_QUEUE_COMPUTE_BIT);
    queueCheckTransfer(inTransferQueue);

    this.graphicsResources =
      RCResourceCollections.create(strings);
    this.transferResources =
      RCResourceCollections.create(strings);
    this.computeResources =
      RCResourceCollections.create(strings);
    this.allResources =
      RCResourceCollections.create(strings);

    /*
     * The order here is significant: Resources are cleaned up in stack order,
     * so the _first_ item added will be the _last_ item popped from the stack.
     */

    this.allResources.add(this.device);
    this.allResources.add(this.allocator);
    this.allResources.add(this.graphicsExecutor);
    this.allResources.add(this.transferExecutor);
    this.allResources.add(this.computeExecutor);

    this.allResources.add(() -> {
      RCExecutors.executeAndWait(this.graphicsExecutor, () -> {
        LOG.debug("Closing graphics resources…");
        this.graphicsResources.close();
        return UNIT;
      });
    });

    this.allResources.add(() -> {
      RCExecutors.executeAndWait(this.transferExecutor, () -> {
        LOG.debug("Closing transfer resources…");
        this.transferResources.close();
        return UNIT;
      });
    });

    this.allResources.add(() -> {
      RCExecutors.executeAndWait(this.computeExecutor, () -> {
        LOG.debug("Closing compute resources…");
        this.computeResources.close();
        return UNIT;
      });
    });

    this.allResources.add(() -> {
      LOG.debug("Waiting for device to idle…");
      this.device.waitIdle();
    });
  }

  private static void queueCheckTransfer(
    final VulkanQueueType queue)
  {
    if (!queue.queueFamilyProperties().queueFlagImpliesTransfer()) {
      throw new IllegalArgumentException(
        "Queue %s has no transfer capability".formatted(queue)
      );
    }
  }

  private static void queueCheck(
    final VulkanQueueType queue,
    final VulkanQueueFamilyPropertyFlag flag)
  {
    if (!queueHas(queue, flag)) {
      throw new IllegalArgumentException(
        "Queue %s has no %s".formatted(queue, flag)
      );
    }
  }

  private static boolean queueHas(
    final VulkanQueueType queue,
    final VulkanQueueFamilyPropertyFlag flag)
  {
    return queue.queueFamilyProperties()
      .queueFlags()
      .contains(flag);
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
    this.allResources.close();
  }

  @Override
  public ExecutorService executorForQueue(
    final VulkanQueueType queue)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(queue, "queue");

    if (Objects.equals(queue, this.graphicsQueue)) {
      return this.graphicsExecutor;
    }
    if (Objects.equals(queue, this.computeQueue)) {
      return this.computeExecutor;
    }
    if (Objects.equals(queue, this.transferQueue)) {
      return this.transferExecutor;
    }
    throw new IllegalArgumentException(
      "Unrecognized queue: %s".formatted(queue)
    );
  }

  @Override
  public RCDeviceQueueCategory categoryForQueue(
    final VulkanQueueType queue)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(queue, "queue");

    if (Objects.equals(queue, this.graphicsQueue)) {
      return RCDeviceQueueCategory.GRAPHICS;
    }
    if (Objects.equals(queue, this.computeQueue)) {
      return RCDeviceQueueCategory.COMPUTE;
    }
    if (Objects.equals(queue, this.transferQueue)) {
      return RCDeviceQueueCategory.TRANSFER;
    }
    throw new IllegalArgumentException(
      "Unrecognized queue: %s".formatted(queue)
    );
  }

  @Override
  public VulkanLogicalDeviceType device()
  {
    return this.device;
  }

  @Override
  public VulkanQueueType graphicsQueue()
  {
    return this.graphicsQueue;
  }

  @Override
  public ExecutorService graphicsExecutor()
  {
    return this.graphicsExecutor;
  }

  @Override
  public VulkanQueueType transferQueue()
  {
    return this.transferQueue;
  }

  @Override
  public ExecutorService transferExecutor()
  {
    return this.transferExecutor;
  }

  @Override
  public VulkanQueueType computeQueue()
  {
    return this.computeQueue;
  }

  @Override
  public ExecutorService computeExecutor()
  {
    return this.computeExecutor;
  }

  @Override
  public <T extends AutoCloseable> T registerGraphicsResource(
    final T closeable)
  {
    return this.graphicsResources.add(closeable);
  }

  @Override
  public <T extends AutoCloseable> T registerTransferResource(
    final T closeable)
  {
    return this.transferResources.add(closeable);
  }

  @Override
  public <T extends AutoCloseable> T registerComputeResource(
    final T closeable)
  {
    return this.computeResources.add(closeable);
  }

  @Override
  public VMAAllocatorType allocator()
  {
    return this.allocator;
  }

  @Override
  public void submitWithFence(
    final RCDeviceQueueCategory category,
    final VulkanFenceType fence,
    final VulkanCommandBufferType... commandBuffers)
    throws RocaroException
  {
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(fence, "fence");
    Objects.requireNonNull(commandBuffers, "commandBuffers");

    final var queue =
      switch (category) {
        case COMPUTE -> this.computeQueue;
        case GRAPHICS -> this.graphicsQueue;
        case TRANSFER -> this.transferQueue;
      };

    final var executor =
      switch (category) {
        case COMPUTE -> this.computeExecutor;
        case GRAPHICS -> this.graphicsExecutor;
        case TRANSFER -> this.transferExecutor;
      };

    RCExecutors.executeAndWait(
      executor,
      () -> {
        queue.submit(
          List.of(
            VulkanSubmitInfo.builder()
              .addCommandBuffers(commandBuffers)
              .build()
          ),
          Optional.of(fence)
        );
        return UNIT;
      }
    );
  }
}
