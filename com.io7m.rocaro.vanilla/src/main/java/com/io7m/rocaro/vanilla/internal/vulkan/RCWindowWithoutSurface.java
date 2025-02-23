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

import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;
import com.io7m.rocaro.vanilla.internal.threading.RCThreadLabels;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;

import java.time.Duration;
import java.util.Objects;

import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;

/**
 * A window that does not have an associated surface (because one is not required).
 *
 * @param window The window
 */

public record RCWindowWithoutSurface(
  RCWindowType window)
  implements RCWindowWithSurfaceType
{
  /**
   * A window that does not have an associated surface (because one is not required).
   *
   * @param window The window
   */

  public RCWindowWithoutSurface
  {
    if (window.requiresSurface()) {
      throw new IllegalArgumentException("Window requires a surface.");
    }
  }

  @RCThread(GPU)
  @Override
  public void close()
    throws RocaroException
  {
    RCThreadLabels.checkThreadLabelsAny(GPU);
    this.window.close();
  }

  @Override
  public RCWindowFrameContextType acquireFrame(
    final RCFrameIndex frame,
    final Duration timeout)
  {
    throw new UnimplementedCodeException();
  }

  @Override
  public int maximumFramesInFlight()
  {
    return 1;
  }

  @Override
  public void configureForPhysicalDevice(
    final VulkanPhysicalDeviceType device)
  {
    Objects.requireNonNull(device, "device");

    throw new UnimplementedCodeException();
  }

  @Override
  public void configureForLogicalDevice(
    final RCDeviceType device,
    final VulkanQueueType graphicsQueue,
    final VulkanQueueType presentationQueue)
  {
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(graphicsQueue, "graphicsQueue");
    Objects.requireNonNull(presentationQueue, "presentationQueue");

    throw new UnimplementedCodeException();
  }
}
