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

import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RocaroException;

import java.util.concurrent.ExecutorService;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.COMPUTE;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.TRANSFER;

/**
 * A device.
 */

public interface RCDeviceType extends RCCloseableType
{
  /**
   * Determine the executor for the given queue.
   *
   * @param queue The queue
   *
   * @return The executor
   *
   * @throws IllegalArgumentException If the queue is not recognized
   */

  ExecutorService executorForQueue(
    VulkanQueueType queue)
    throws IllegalArgumentException;

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
   * @return The executor for graphics operations
   */

  ExecutorService graphicsExecutor();

  /**
   * @return The queue for transfer operations
   */

  VulkanQueueType transferQueue();

  /**
   * @return The executor for transfer operations
   */

  ExecutorService transferExecutor();

  /**
   * @return The queue for compute operations
   */

  VulkanQueueType computeQueue();

  /**
   * @return The executor for compute operations
   */

  ExecutorService computeExecutor();

  /**
   * Register a graphics resource that will be closed just prior to the device
   * being closed.
   *
   * @param closeable The resource
   * @param <T>       The type of resource
   *
   * @return The resource
   */

  <T extends AutoCloseable>
  T registerGraphicsResource(
    T closeable);

  /**
   * Register a transfer resource that will be closed just prior to the device
   * being closed.
   *
   * @param closeable The resource
   * @param <T>       The type of resource
   *
   * @return The resource
   */

  <T extends AutoCloseable>
  T registerTransferResource(
    T closeable);

  /**
   * Register a compute resource that will be closed just prior to the device
   * being closed.
   *
   * @param closeable The resource
   * @param <T>       The type of resource
   *
   * @return The resource
   */

  <T extends AutoCloseable>
  T registerComputeResource(
    T closeable);

  /**
   * Register a resource that will be closed just prior to the device
   * being closed.
   *
   * @param closeable The resource
   * @param category  The queue category
   * @param <T>       The type of resource
   *
   * @return The resource
   */

  default <T extends AutoCloseable>
  T registerResource(
    final RCDeviceQueueCategory category,
    final T closeable)
  {
    return switch (category) {
      case COMPUTE -> this.registerComputeResource(closeable);
      case GRAPHICS -> this.registerGraphicsResource(closeable);
      case TRANSFER -> this.registerTransferResource(closeable);
    };
  }

  /**
   * @return The VMA allocator
   */

  VMAAllocatorType allocator();

  /**
   * Submit work to one of the device queues.
   *
   * @param category       The queue category
   * @param fence          The fence signalled when the work is completed
   * @param commandBuffers The command buffers
   *
   * @throws RocaroException On errors
   */

  void submitWithFence(
    RCDeviceQueueCategory category,
    VulkanFenceType fence,
    VulkanCommandBufferType... commandBuffers)
    throws RocaroException;

  /**
   * Submit work to the device transfer queue.
   *
   * @param fence          The fence signalled when the work is completed
   * @param commandBuffers The command buffers
   *
   * @throws RocaroException On errors
   */

  default void transferSubmitWithFence(
    final VulkanFenceType fence,
    final VulkanCommandBufferType... commandBuffers)
    throws RocaroException
  {
    this.submitWithFence(TRANSFER, fence, commandBuffers);
  }

  /**
   * Submit work to the device graphics queue.
   *
   * @param fence          The fence signalled when the work is completed
   * @param commandBuffers The command buffers
   *
   * @throws RocaroException On errors
   */

  default void graphicsSubmitWithFence(
    final VulkanFenceType fence,
    final VulkanCommandBufferType... commandBuffers)
    throws RocaroException
  {
    this.submitWithFence(GRAPHICS, fence, commandBuffers);
  }

  /**
   * Submit work to the device compute queue.
   *
   * @param fence          The fence signalled when the work is completed
   * @param commandBuffers The command buffers
   *
   * @throws RocaroException On errors
   */

  default void computeSubmitWithFence(
    final VulkanFenceType fence,
    final VulkanCommandBufferType... commandBuffers)
    throws RocaroException
  {
    this.submitWithFence(COMPUTE, fence, commandBuffers);
  }
}
