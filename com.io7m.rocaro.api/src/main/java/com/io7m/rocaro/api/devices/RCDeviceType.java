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


package com.io7m.rocaro.api.devices;

import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RocaroException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A device.
 */

public interface RCDeviceType extends RCCloseableType
{
  /**
   * Get the executor for device operations.
   *
   * @return The executor
   */

  Executor gpuExecutor();

  /**
   * Determine the category for the given queue.
   *
   * @param queue The queue
   *
   * @return The category
   *
   * @throws IllegalArgumentException If the queue is not recognized
   */

  RCDeviceQueueCategory categoryForQueue(
    VulkanQueueType queue)
    throws IllegalArgumentException;

  /**
   * @return The underlying Vulkan logical device
   */

  VulkanLogicalDeviceType device();

  /**
   * @return The queue for graphics operations
   */

  VulkanQueueType graphicsQueue();

  /**
   * @return The queue for transfer operations
   */

  VulkanQueueType transferQueue();

  /**
   * @return The queue for compute operations
   */

  VulkanQueueType computeQueue();

  /**
   * Register a GPU resource that will be closed just prior to the device
   * being closed. The resource will be closed on the GPU thread.
   *
   * @param closeable The resource
   * @param <T>       The type of resource
   *
   * @return The resource
   */

  <T extends AutoCloseable>
  T registerResource(
    T closeable);

  /**
   * @return The VMA allocator
   */

  VMAAllocatorType allocator();

  /**
   * @param category The queue category
   *
   * @return The queue for the given queue category
   */

  default VulkanQueueType queueForCategory(
    final RCDeviceQueueCategory category)
  {
    return switch (category) {
      case COMPUTE -> this.computeQueue();
      case GRAPHICS -> this.graphicsQueue();
      case TRANSFER -> this.transferQueue();
    };
  }

  /**
   * Submit work to the given queue category. The work is submitted
   * on the device executor. The operation returns a future that will be
   * completed when the submission is completed. Note that the submission
   * being completed _does not_ mean the work is completed; use the optional
   * fence to detect the completion of work.
   *
   * @param category   The queue category
   * @param submission The work
   * @param fence      The fence to signal, if any
   *
   * @return The operation in progress
   */

  default CompletableFuture<RCUnit> submit(
    final RCDeviceQueueCategory category,
    final List<VulkanSubmitInfo> submission,
    final Optional<VulkanFenceType> fence)
  {
    return this.submit(
      this.queueForCategory(category),
      submission,
      fence
    );
  }

  /**
   * Submit work to the given queue. The work is submitted
   * on the device executor. The operation returns a future that will be
   * completed when the submission is completed. Note that the submission
   * being completed _does not_ mean the work is completed; use the optional
   * fence to detect the completion of work.
   *
   * @param queue      The queue
   * @param submission The work
   * @param fence      The fence to signal, if any
   *
   * @return The operation in progress
   */

  CompletableFuture<RCUnit> submit(
    VulkanQueueType queue,
    List<VulkanSubmitInfo> submission,
    Optional<VulkanFenceType> fence
  );

  /**
   * Execute work on the device executor.
   *
   * @param operation The operation
   * @param <T>       The type of results
   *
   * @return The operation in progress
   */

  <T> CompletableFuture<T> execute(
    Callable<T> operation
  );

  /**
   * Wait until all queues on the device are idle.
   *
   * @throws RocaroException On errors
   */

  void waitUntilIdle()
    throws RocaroException;
}
