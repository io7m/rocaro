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
import com.io7m.jcoronado.api.VulkanExtent2D;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType.VulkanKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanSurfaceCapabilitiesKHR;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanSurfaceFormatKHR;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_B8G8R8A8_UNORM;
import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_UNDEFINED;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR;

/**
 * A window along with the surface needed to render to it, and the
 * various bits of Vulkan state associated with it.
 */

public final class RCWindowWithSurface
  implements RCWindowWithSurfaceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCWindowWithSurface.class);

  private final RCWindowType window;
  private final VulkanExtKHRSurfaceType khrSurfaceExt;
  private final VulkanKHRSurfaceType surface;
  private VulkanSurfaceCapabilitiesKHR surfaceCaps;
  private VulkanPresentModeKHR surfacePresent;
  private VulkanSurfaceFormatKHR surfaceFormat;
  private VulkanExtent2D surfaceExtent;
  private VulkanQueueType presentationQueue;

  /**
   * Construct a window.
   *
   * @param inWindow        The window
   * @param inKHRSurfaceExt The KHR surface extension
   * @param inSurface       The surface
   */

  public RCWindowWithSurface(
    final RCWindowType inWindow,
    final VulkanExtKHRSurfaceType inKHRSurfaceExt,
    final VulkanKHRSurfaceType inSurface)
  {
    if (!inWindow.requiresSurface()) {
      throw new IllegalArgumentException("Window does not require a surface.");
    }

    this.window =
      Objects.requireNonNull(inWindow, "window");
    this.khrSurfaceExt =
      Objects.requireNonNull(inKHRSurfaceExt, "khrSurfaceExt");
    this.surface =
      Objects.requireNonNull(inSurface, "surface");
  }

  private static void showPreferredFormats(
    final List<VulkanSurfaceFormatKHR> formats)
  {
    for (int index = 0; index < formats.size(); ++index) {
      LOG.debug(
        "Preferred surface format [{}]: {}",
        index,
        formats.get(index)
      );
    }
  }

  private static void showAvailablePresentationModes(
    final List<VulkanPresentModeKHR> modes)
  {
    for (int index = 0; index < modes.size(); ++index) {
      LOG.debug(
        "Available presentation mode [{}]: {}",
        index,
        modes.get(index)
      );
    }
  }

  @Override
  public void close()
    throws RocaroException
  {
    this.window.close();

    try {
      this.surface.close();
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  @Override
  public RCWindowType window()
  {
    return this.window;
  }

  @Override
  public void configureForPhysicalDevice(
    final VulkanPhysicalDeviceType device)
    throws RCVulkanException
  {
    Objects.requireNonNull(device, "device");

    try {
      this.surfaceFormat = this.pickSurfaceFormat(device);
      LOG.debug("Selected surface format: {}", this.surfaceFormat);

      this.surfacePresent = this.pickPresentationMode(device);
      LOG.debug("Selected presentation mode: {}", this.surfacePresent);

      this.surfaceCaps =
        this.khrSurfaceExt.surfaceCapabilities(device, this.surface);
      LOG.debug(
        "Surface [MinImages]               {}",
        this.surfaceCaps.minImageCount()
      );
      LOG.debug(
        "Surface [MaxImages]               {}",
        this.surfaceCaps.maxImageCount()
      );
      LOG.debug(
        "Surface [SupportedCompositeAlpha] {}",
        this.surfaceCaps.supportedCompositeAlpha()
      );
      LOG.debug(
        "Surface [SupportedUsageFlags]     {}",
        this.surfaceCaps.supportedUsageFlags()
      );
      LOG.debug(
        "Surface [SupportedTransforms]     {}",
        this.surfaceCaps.supportedTransforms()
      );

      this.surfaceExtent = this.pickExtent();
      LOG.debug("Surface extent: {}", this.surfaceExtent);
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  /**
   * Work out the extent of the rendered image based on the
   * implementation-defined supported limits.
   */

  private VulkanExtent2D pickExtent()
  {
    if (this.surfaceCaps.currentExtent().width() != 0xffff_ffff) {
      return this.surfaceCaps.currentExtent();
    }

    final var width =
      Math.clamp(
        this.window.width(),
        this.surfaceCaps.minImageExtent().width(),
        this.surfaceCaps.maxImageExtent().width()
      );

    final var height =
      Math.clamp(
        this.window.height(),
        this.surfaceCaps.minImageExtent().height(),
        this.surfaceCaps.maxImageExtent().height()
      );

    return VulkanExtent2D.of(width, height);
  }

  /**
   * Pick the best presentation mode available.
   */

  private VulkanPresentModeKHR pickPresentationMode(
    final VulkanPhysicalDeviceType device)
    throws VulkanException
  {
    final var modes =
      this.khrSurfaceExt.surfacePresentModes(device, this.surface);

    showAvailablePresentationModes(modes);

    var preferred = VK_PRESENT_MODE_FIFO_KHR;
    for (final var mode : modes) {
      if (mode == VK_PRESENT_MODE_MAILBOX_KHR) {
        return mode;
      }
      if (mode == VK_PRESENT_MODE_IMMEDIATE_KHR) {
        preferred = mode;
      }
    }

    return preferred;
  }

  private VulkanSurfaceFormatKHR pickSurfaceFormat(
    final VulkanPhysicalDeviceType device)
    throws VulkanException
  {
    final var formats =
      this.khrSurfaceExt.surfaceFormats(device, this.surface);

    /*
     * If there are no formats, try a commonly supported one.
     */

    if (formats.isEmpty()) {
      LOG.debug("There are no preferred surface formats.");
      return VulkanSurfaceFormatKHR.of(
        VK_FORMAT_B8G8R8A8_UNORM,
        VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
      );
    }

    showPreferredFormats(formats);

    /*
     * If there's one VK_FORMAT_UNDEFINED format, then this means that the implementation
     * doesn't have a preferred format and anything can be used.
     */

    if (formats.size() == 1) {
      final var format0 = formats.get(0);
      if (format0.format() == VK_FORMAT_UNDEFINED) {
        LOG.debug("The one preferred surface format is VK_FORMAT_UNDEFINED.");
        return VulkanSurfaceFormatKHR.of(
          VK_FORMAT_B8G8R8A8_UNORM,
          VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        );
      }
    }

    /*
     * Otherwise, look for a linear BGRA unsigned normalized format, with an SRGB color space.
     */

    LOG.debug("Searching within the preferred surface formats.");
    for (final var format : formats) {
      if (format.format() == VK_FORMAT_B8G8R8A8_UNORM
          && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
        return format;
      }
    }

    /*
     * Otherwise, use whatever was first.
     */

    LOG.debug("Fell back to first available format.");
    return formats.get(0);
  }

  /**
   * @return The surface extension
   */

  public VulkanExtKHRSurfaceType khrSurfaceExt()
  {
    return this.khrSurfaceExt;
  }

  /**
   * @return The actual surface
   */

  public VulkanKHRSurfaceType surface()
  {
    return this.surface;
  }

  /**
   * Set the queue used for presentation operations.
   *
   * @param inPresentationQueue The presentation queue
   */

  public void setPresentationQueue(
    final VulkanQueueType inPresentationQueue)
  {
    this.presentationQueue =
      Objects.requireNonNull(inPresentationQueue, "presentationQueue");
  }
}
