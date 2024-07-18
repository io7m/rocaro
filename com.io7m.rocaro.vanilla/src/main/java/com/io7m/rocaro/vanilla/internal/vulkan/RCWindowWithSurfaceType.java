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
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;

/**
 * The type of windows that may have attached rendering surfaces.
 */

public sealed interface RCWindowWithSurfaceType
  extends AutoCloseable
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

  @Override
  void close()
    throws RocaroException;
}
