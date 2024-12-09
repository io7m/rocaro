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
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The type of windows that may have attached rendering surfaces.
 */

public sealed interface RCWindowWithSurfaceType
  extends RCCloseableType
  permits RCWindowWithSurface,
  RCWindowWithoutSurface
{
  /**
   * @return The window
   */

  RCWindowType window();

  /**
   * Configure the window for the given physical device.
   *
   * @param device The device
   *
   * @throws RocaroException On errors
   */

  void configureForPhysicalDevice(
    VulkanPhysicalDeviceType device)
    throws RocaroException;

  /**
   * Configure the window for the given logical device.
   *
   * @param device            The device
   * @param graphicsQueue     The graphics queue
   * @param presentationQueue The presentation queue
   *
   * @throws RocaroException On errors
   */

  void configureForLogicalDevice(
    RCDeviceType device,
    VulkanQueueType graphicsQueue,
    VulkanQueueType presentationQueue)
    throws RocaroException;

  @Override
  void close()
    throws RocaroException;

  /**
   * Acquire a frame for rendering.
   *
   * @param frame   The frame index
   * @param timeout The timeout value
   *
   * @return The frame context
   *
   * @throws RCVulkanException On errors
   * @throws TimeoutException  If a frame cannot be acquired within a timeout
   */

  RCWindowFrameContextType acquireFrame(
    RCFrameIndex frame,
    Duration timeout)
    throws RCVulkanException, TimeoutException;

  /**
   * @return The maximum number of frames in flight
   */

  int maximumFramesInFlight();
}
