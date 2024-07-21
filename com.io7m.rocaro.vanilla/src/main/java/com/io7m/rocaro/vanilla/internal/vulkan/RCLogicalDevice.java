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

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.rocaro.api.RCCloseableType;

import java.util.Objects;

import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_COMPUTE_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;

/**
 * A logical device.
 *
 * @param device        The device
 * @param graphicsQueue The queue for graphics operations
 * @param transferQueue The queue for transfer operations
 * @param computeQueue  The queue for compute operations
 */

public record RCLogicalDevice(
  VulkanLogicalDeviceType device,
  VulkanQueueType graphicsQueue,
  VulkanQueueType transferQueue,
  VulkanQueueType computeQueue)
  implements RCCloseableType
{
  /**
   * A logical device.
   *
   * @param device        The device
   * @param graphicsQueue The queue for graphics operations
   * @param transferQueue The queue for transfer operations
   * @param computeQueue  The queue for compute operations
   */

  public RCLogicalDevice
  {
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(graphicsQueue, "graphicsQueue");
    Objects.requireNonNull(transferQueue, "transferQueue");
    Objects.requireNonNull(computeQueue, "computeQueue");

    queueCheck(graphicsQueue, VK_QUEUE_GRAPHICS_BIT);
    queueCheck(computeQueue, VK_QUEUE_COMPUTE_BIT);
    queueCheckTransfer(transferQueue);
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
    throws RCVulkanException
  {
    try {
      this.device.close();
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }
}
