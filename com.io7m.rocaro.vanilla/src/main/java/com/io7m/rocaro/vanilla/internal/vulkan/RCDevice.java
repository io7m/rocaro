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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceJFREventQueueSubmit;
import com.io7m.rocaro.api.devices.RCDeviceJFREventWaitIdle;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutorType;
import com.io7m.rocaro.vanilla.internal.threading.RCThreadLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_COMPUTE_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.rocaro.api.RCUnit.UNIT;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;

/**
 * A device.
 */

public final class RCDevice
  extends RCObject
  implements RCDeviceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCDevice.class);

  private final CloseableCollectionType<RocaroException> allResources;
  private final RCExecutorType executor;
  private final VulkanLogicalDeviceType device;
  private final VulkanQueueType computeQueue;
  private final VulkanQueueType graphicsQueue;
  private final VulkanQueueType transferQueue;
  private final CloseableCollectionType<RocaroException> gpuResources;
  private VMAAllocatorType allocator;

  RCDevice(
    final RCStrings strings,
    final VulkanLogicalDeviceType inDevice,
    final RCExecutorType inExecutor,
    final VulkanQueueType inGraphicsQueue,
    final VulkanQueueType inTransferQueue,
    final VulkanQueueType inComputeQueue)
  {
    this.device =
      Objects.requireNonNull(inDevice, "device");
    this.executor =
      Objects.requireNonNull(inExecutor, "inExecutor");
    this.graphicsQueue =
      Objects.requireNonNull(inGraphicsQueue, "graphicsQueue");
    this.transferQueue =
      Objects.requireNonNull(inTransferQueue, "transferQueue");
    this.computeQueue =
      Objects.requireNonNull(inComputeQueue, "computeQueue");

    queueCheck(inGraphicsQueue, VK_QUEUE_GRAPHICS_BIT);
    queueCheck(inComputeQueue, VK_QUEUE_COMPUTE_BIT);
    queueCheckTransfer(inTransferQueue);

    this.gpuResources =
      RCResourceCollections.create(strings);
    this.allResources =
      RCResourceCollections.create(strings);

    /*
     * The order here is significant: Resources are cleaned up in stack order,
     * so the _first_ item added will be the _last_ item popped from the stack.
     */

    this.allResources.add(this.device);
    this.allResources.add(this.executor);

    this.allResources.add(() -> {
      LOG.debug("Closing GPU resources…");
      this.gpuResources.close();
    });

    this.allResources.add(this::waitUntilIdle);
  }

  @Override
  public void waitUntilIdle()
    throws RCVulkanException
  {
    LOG.debug("Waiting for device to idle…");

    final var ev = new RCDeviceJFREventWaitIdle();
    ev.begin();

    try {
      this.device.waitIdle();
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    } finally {
      ev.end();
      ev.commit();
    }
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
    RCThreadLabels.checkThreadLabelsAny(GPU);
    this.allResources.close();
  }

  @Override
  public RCExecutorType gpuExecutor()
  {
    return this.executor;
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
  public VulkanQueueType transferQueue()
  {
    return this.transferQueue;
  }

  @Override
  public VulkanQueueType computeQueue()
  {
    return this.computeQueue;
  }

  @Override
  public <T extends AutoCloseable> T registerResource(
    final T closeable)
  {
    this.gpuResources.add(closeable);
    return closeable;
  }

  @Override
  public VMAAllocatorType allocator()
  {
    Preconditions.checkPreconditionV(
      this.allocator != null,
      "Allocator must have been assigned."
    );
    return this.allocator;
  }

  @Override
  public CompletableFuture<RCUnit> submit(
    final VulkanQueueType queue,
    final List<VulkanSubmitInfo> submission,
    final Optional<VulkanFenceType> fence)
  {
    Objects.requireNonNull(queue, "queue");
    Objects.requireNonNull(fence, "fence");
    Objects.requireNonNull(submission, "submission");

    return this.execute(() -> {
      if (LOG.isTraceEnabled()) {
        logSubmission(queue, submission, fence);
      }

      final var ev = new RCDeviceJFREventQueueSubmit();
      ev.queue = queue.toString();
      ev.begin();

      try {
        queue.submit(submission, fence);
      } finally {
        ev.end();
        ev.commit();
      }
      return UNIT;
    });
  }

  @Override
  public <T> CompletableFuture<T> execute(
    final Callable<T> operation)
  {
    Objects.requireNonNull(operation, "operation");

    final var future = new CompletableFuture<T>();
    this.executor.execute(() -> {
      try {
        future.complete(operation.call());
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  private static void logSubmission(
    final VulkanQueueType queue,
    final List<VulkanSubmitInfo> submission,
    final Optional<VulkanFenceType> fence)
  {
    final var text = new StringBuilder(256);
    text.append("%nBegin Submission%n".formatted());
    text.append("  * Queue:            %s%n".formatted(queue));

    for (final var sub : submission) {
      for (final var buffer : sub.commandBuffers()) {
        text.append("  * Command Buffer:   %s%n".formatted(buffer.commandBuffer()));
      }
      for (final var semaphore : sub.waitSemaphores()) {
        text.append(
          "  * Wait Semaphore:   %s (%s)%n"
            .formatted(semaphore.semaphore(), semaphore.stageMask())
        );
      }
      for (final var semaphore : sub.signalSemaphores()) {
        text.append(
          "  * Signal Semaphore: %s (%s)%n"
            .formatted(semaphore.semaphore(), semaphore.stageMask())
        );
      }
      text.append("  * Fence:            %s%n".formatted(fence));
    }
    text.append("End Submission");
    LOG.trace("{}", text);
  }

  /**
   * Set the allocator.
   *
   * @param vmaAllocator The allocator
   */

  public void setAllocator(
    final VMAAllocatorType vmaAllocator)
  {
    this.allocator =
      this.allResources.add(
        Objects.requireNonNull(vmaAllocator, "vmaAllocator")
      );
  }
}
