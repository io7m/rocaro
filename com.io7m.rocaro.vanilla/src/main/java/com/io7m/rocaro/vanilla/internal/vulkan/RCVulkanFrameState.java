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

import com.io7m.jcoronado.api.VulkanCommandPoolCreateInfo;
import com.io7m.jcoronado.api.VulkanCommandPoolType;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;

import java.util.Objects;

/**
 * The state used for a single frame.
 */

public final class RCVulkanFrameState
  implements RCVulkanFrameStateType
{
  private final RCFrameIndex index;
  private final CloseableCollectionType<RocaroException> resources;
  private final VulkanCommandPoolType commandPool;

  private RCVulkanFrameState(
    final RCFrameIndex inIndex,
    final CloseableCollectionType<RocaroException> inResources,
    final VulkanCommandPoolType inCommandPool)
  {
    this.index =
      Objects.requireNonNull(inIndex, "index");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.commandPool =
      Objects.requireNonNull(inCommandPool, "commandPool");
  }

  /**
   * Create the state required to render a single frame.
   *
   * @param strings       The string resources
   * @param logicalDevice The logical device
   * @param inFrameIndex  The frame index
   *
   * @return The state
   *
   * @throws RCVulkanException On errors
   */

  public static RCVulkanFrameStateType create(
    final RCStrings strings,
    final RCDevice logicalDevice,
    final RCFrameIndex inFrameIndex)
    throws RCVulkanException
  {
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(logicalDevice, "logicalDevice");
    Objects.requireNonNull(inFrameIndex, "frameIndex");

    try {
      final var device =
        logicalDevice.device();

      final var resources =
        RCResourceCollections.create(strings);

      final var commandPool =
        resources.add(
          device.createCommandPool(
            VulkanCommandPoolCreateInfo.builder()
              .setQueueFamilyIndex(
                logicalDevice.graphicsQueue().queueFamilyIndex())
              .build()
          )
        );

      device.debugging()
        .setObjectName(commandPool, "CommandPool[FrameGraphics]");

      return new RCVulkanFrameState(
        inFrameIndex,
        resources,
        commandPool
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  @Override
  public void close()
    throws RocaroException
  {
    this.resources.close();
  }

  @Override
  public VulkanCommandPoolType commandPool()
  {
    return this.commandPool;
  }
}
