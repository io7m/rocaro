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

import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanSemaphoreType;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetType;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;

import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;

/**
 * The context of a single frame.
 */

public interface RCWindowFrameContextType
{
  /**
   * Obtain a reference to a semaphore that will be signalled when the image
   * is ready to be rendered to.
   *
   * @return The image-ready semaphore
   */

  VulkanSemaphoreType imageIsReady();

  /**
   * Obtain a reference to a semaphore that will be signalled when rendering
   * is finished.
   *
   * @return The rendering-done semaphore
   */

  VulkanSemaphoreType imageRenderingIsFinished();

  /**
   * Obtain a reference to a fence that will be signalled when rendering
   * is finished.
   *
   * @return The rendering-done fence
   */

  VulkanFenceType imageRenderingIsFinishedFence();

  /**
   * @return The image to which to render
   */

  RCPresentationRenderTargetType image();

  /**
   * Signal that it is time to present the image.
   *
   * @throws RCVulkanException On errors
   */

  @RCThread(GPU)
  void present()
    throws RCVulkanException;
}
