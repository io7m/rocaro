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

import com.io7m.jcoronado.api.VulkanComponentMapping;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent2D;
import com.io7m.jcoronado.api.VulkanFenceCreateFlag;
import com.io7m.jcoronado.api.VulkanFenceCreateInfo;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanImageSubresourceRange;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewCreateFlag;
import com.io7m.jcoronado.api.VulkanImageViewCreateInfo;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSemaphoreCreateInfo;
import com.io7m.jcoronado.api.VulkanSemaphoreType;
import com.io7m.jcoronado.api.VulkanSharingMode;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType.VulkanKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanSurfaceCapabilitiesKHR;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanSurfaceFormatKHR;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType.VulkanKHRSwapChainType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentInfoKHR;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanSwapChainCreateInfo;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.images.RCImageColorBlendable;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.jcoronado.api.VulkanComponentSwizzle.VK_COMPONENT_SWIZZLE_IDENTITY;
import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_B8G8R8A8_UNORM;
import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_UNDEFINED;
import static com.io7m.jcoronado.api.VulkanImageAspectFlag.VK_IMAGE_ASPECT_COLOR_BIT;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static com.io7m.jcoronado.api.VulkanImageViewKind.VK_IMAGE_VIEW_TYPE_2D;
import static com.io7m.jcoronado.api.VulkanSharingMode.VK_SHARING_MODE_CONCURRENT;
import static com.io7m.jcoronado.api.VulkanSharingMode.VK_SHARING_MODE_EXCLUSIVE;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanCompositeAlphaFlagKHR.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.EXTENSION;

/**
 * A window along with the surface needed to render to it, and the
 * various bits of Vulkan state associated with it.
 */

public final class RCWindowWithSurface
  extends RCObject
  implements RCWindowWithSurfaceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCWindowWithSurface.class);

  private final RCWindowType window;
  private final VulkanExtKHRSurfaceType khrSurfaceExt;
  private final VulkanKHRSurfaceType surface;
  private final RCStrings strings;
  private final AtomicBoolean closed;
  private final Map<SwapChainIndex, VulkanImageViewType> swapChainImageViews;
  private final CloseableCollectionType<RocaroException> resources;
  private final CloseableCollectionType<RocaroException> resourcesPerSwapChain;
  private final Map<SwapChainIndex, VulkanImageType> swapChainImages;
  private final Map<RCFrameIndex, VulkanSemaphoreType> swapChainImageReadySemaphores;
  private final Map<RCFrameIndex, VulkanSemaphoreType> swapChainImageRenderingDoneSemaphores;
  private final Map<RCFrameIndex, VulkanFenceType> swapChainImageRenderingDoneFences;
  private VulkanSurfaceCapabilitiesKHR surfaceCaps;
  private VulkanPresentModeKHR surfacePresent;
  private VulkanSurfaceFormatKHR surfaceFormat;
  private VulkanExtent2D surfaceExtent;
  private VulkanQueueType presentationQueue;
  private VulkanLogicalDeviceType device;
  private VulkanQueueType graphicsQueue;
  private VulkanExtKHRSwapChainType khrSwapChainExt;
  private VulkanKHRSwapChainType swapChain;

  /**
   * Construct a window.
   *
   * @param inStrings       The string resources
   * @param inWindow        The window
   * @param inKHRSurfaceExt The KHR surface extension
   * @param inSurface       The surface
   */

  public RCWindowWithSurface(
    final RCStrings inStrings,
    final RCWindowType inWindow,
    final VulkanExtKHRSurfaceType inKHRSurfaceExt,
    final VulkanKHRSurfaceType inSurface)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");

    if (!inWindow.requiresSurface()) {
      throw new IllegalArgumentException("Window does not require a surface.");
    }

    this.window =
      Objects.requireNonNull(inWindow, "window");
    this.khrSurfaceExt =
      Objects.requireNonNull(inKHRSurfaceExt, "khrSurfaceExt");
    this.surface =
      Objects.requireNonNull(inSurface, "surface");

    this.swapChainImages =
      new TreeMap<>();
    this.swapChainImageViews =
      new TreeMap<>();
    this.swapChainImageReadySemaphores =
      new TreeMap<>();
    this.swapChainImageRenderingDoneSemaphores =
      new TreeMap<>();
    this.swapChainImageRenderingDoneFences =
      new TreeMap<>();

    this.resources =
      RCResourceCollections.create(this.strings);
    this.resourcesPerSwapChain =
      RCResourceCollections.create(this.strings);
    this.closed =
      new AtomicBoolean(false);

    this.resources.add(this.surface);
    this.resources.add(this.window);
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
    if (this.closed.compareAndSet(false, true)) {
      try {
        LOG.debug("Waiting for device to become idle...");
        this.device.waitIdle();
      } catch (final VulkanException e) {
        throw RCVulkanException.wrap(e);
      }

      try {
        this.resourcesPerSwapChain.close();
      } finally {
        this.resources.close();
      }
    }
  }

  private static final class FrameContext
    implements RCWindowFrameContextType
  {
    private final RCImageColorBlendableType image;
    private final RCWindowWithSurface windowWithSurface;
    private final SwapChainIndex index;
    private final VulkanFenceType imageRenderingIsFinishedFence;
    private final VulkanSemaphoreType imageIsReadySemaphore;
    private final VulkanSemaphoreType imageRenderingIsFinishedSemaphore;

    private FrameContext(
      final RCWindowWithSurface inWindowWithSurface,
      final SwapChainIndex inIndex,
      final VulkanSemaphoreType inImageIsReadySemaphore,
      final VulkanSemaphoreType inImageRenderingIsFinishedSemaphore,
      final VulkanFenceType inImageRenderingIsFinishedFence,
      final RCImageColorBlendableType inImage)
    {
      this.windowWithSurface =
        Objects.requireNonNull(inWindowWithSurface, "windowWithSurface");
      this.index =
        Objects.requireNonNull(inIndex, "inIndex");

      this.imageIsReadySemaphore =
        Objects.requireNonNull(
          inImageIsReadySemaphore,
          "imageIsReadySemaphore"
        );
      this.imageRenderingIsFinishedSemaphore =
        Objects.requireNonNull(
          inImageRenderingIsFinishedSemaphore,
          "imageRenderingIsFinishedSemaphore"
        );
      this.image =
        Objects.requireNonNull(inImage, "image");
      this.imageRenderingIsFinishedFence =
        Objects.requireNonNull(
          inImageRenderingIsFinishedFence,
          "imageRenderingIsFinishedFence");
    }

    @Override
    public VulkanSemaphoreType imageIsReady()
    {
      return this.imageIsReadySemaphore;
    }

    @Override
    public VulkanSemaphoreType imageRenderingIsFinished()
    {
      return this.imageRenderingIsFinishedSemaphore;
    }

    @Override
    public VulkanFenceType imageRenderingIsFinishedFence()
    {
      return this.imageRenderingIsFinishedFence;
    }

    @Override
    public RCImageColorBlendableType image()
    {
      return this.image;
    }

    @Override
    public void present()
      throws RCVulkanException
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Presenting.");
      }

      try {
        final var presentationInfo =
          VulkanPresentInfoKHR.builder()
            .addImageIndices(this.index.index)
            .addSwapChains(this.windowWithSurface.swapChain)
            .addWaitSemaphores(this.imageRenderingIsFinishedSemaphore)
            .build();

        this.windowWithSurface.khrSwapChainExt.queuePresent(
          this.windowWithSurface.presentationQueue, presentationInfo
        );
      } catch (final VulkanException e) {
        throw RCVulkanException.wrap(e);
      }
    }

    @Override
    public void close()
    {

    }
  }

  @Override
  public RCWindowFrameContextType acquireFrame(
    final RCFrameIndex frameIndex,
    final Duration timeout)
    throws RCVulkanException, TimeoutException
  {
    Objects.requireNonNull(frameIndex, "frameIndex");
    Objects.requireNonNull(timeout, "timeout");

    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Acquiring frame {}", frameIndex);
      }

      final var readySemaphore =
        this.swapChainImageReadySemaphores.get(frameIndex);
      final var renderDoneSemaphore =
        this.swapChainImageRenderingDoneSemaphores.get(frameIndex);
      final var renderDoneFence =
        this.swapChainImageRenderingDoneFences.get(frameIndex);

      Objects.requireNonNull(readySemaphore, "readySemaphore");
      Objects.requireNonNull(renderDoneSemaphore, "renderDoneSemaphore");
      Objects.requireNonNull(renderDoneFence, "renderDoneFence");

      final var waitStatus =
        this.device.waitForFence(
          renderDoneFence,
          timeout.toNanos()
        );

      switch (waitStatus) {
        case VK_WAIT_SUCCEEDED -> {

        }
        case VK_WAIT_TIMED_OUT -> {
          throw new TimeoutException("Image acquisition fence timed out.");
        }
      }

      this.device.resetFences(List.of(renderDoneFence));

      final var acquisition =
        this.swapChain.acquireImageWithSemaphore(
          timeout.toNanos(),
          readySemaphore
        );

      if (acquisition.timedOut()) {
        throw new TimeoutException("Image acquisition timed out.");
      }

      final var swapIndex =
        new SwapChainIndex(acquisition.imageIndex().orElseThrow());

      if (LOG.isTraceEnabled()) {
        LOG.trace("Swapchain image index {}", swapIndex);
      }

      final var imageData =
        this.swapChainImages.get(swapIndex);
      final var imageView =
        this.swapChainImageViews.get(swapIndex);

      final var image =
        new RCImageColorBlendable(
          this.window.size(),
          imageData,
          imageView,
          this.surfaceFormat.format()
        );

      return new FrameContext(
        this,
        swapIndex,
        readySemaphore,
        renderDoneSemaphore,
        renderDoneFence,
        image
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  @Override
  public int maximumFramesInFlight()
  {
    return this.swapChainImages.size();
  }

  @Override
  public RCWindowType window()
  {
    return this.window;
  }

  @Override
  public void configureForPhysicalDevice(
    final VulkanPhysicalDeviceType newPhysicalDevice)
    throws RCVulkanException
  {
    Objects.requireNonNull(newPhysicalDevice, "device");

    try {
      this.surfaceFormat = this.pickSurfaceFormat(newPhysicalDevice);
      LOG.debug("Selected surface format: {}", this.surfaceFormat);
      Objects.requireNonNull(this.surfaceFormat, "surfaceFormat");

      this.surfacePresent = this.pickPresentationMode(newPhysicalDevice);
      LOG.debug("Selected presentation mode: {}", this.surfacePresent);
      Objects.requireNonNull(this.surfacePresent, "surfacePresent");

      this.surfaceCaps =
        this.khrSurfaceExt.surfaceCapabilities(newPhysicalDevice, this.surface);
      Objects.requireNonNull(this.surfaceCaps, "surfaceCaps");

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
      Objects.requireNonNull(this.surfaceExtent, "surfaceExtent");
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  @Override
  public void configureForLogicalDevice(
    final VulkanLogicalDeviceType newDevice,
    final VulkanQueueType newGraphicsQueue,
    final VulkanQueueType newPresentationQueue)
    throws RocaroException
  {
    this.device =
      Objects.requireNonNull(newDevice, "device");
    this.graphicsQueue =
      Objects.requireNonNull(newGraphicsQueue, "graphicsQueue");
    this.presentationQueue =
      Objects.requireNonNull(newPresentationQueue, "presentationQueue");

    try {
      final var debugging =
        this.device.debugging();

      this.khrSwapChainExt =
        newDevice.findEnabledExtension(
            "VK_KHR_swapchain", VulkanExtKHRSwapChainType.class)
          .orElseThrow(() -> {
            return this.errorMissingRequiredException("VK_KHR_swapchain");
          });

      final var minimumImageCount =
        this.pickMinimumImageCount();
      final List<VulkanQueueFamilyIndex> queueIndices =
        new ArrayList<>();
      final var imageSharingMode =
        this.pickImageSharingMode(queueIndices);
      final var imageUsageFlags =
        Set.of(
          VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
          VK_IMAGE_USAGE_TRANSFER_DST_BIT
        );
      final var surfaceAlphaFlags =
        Set.of(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

      LOG.debug("Swap chain (minimum) image count: {}", minimumImageCount);
      LOG.debug("Swap chain image mode: {}", imageSharingMode);

      final var swapChainCreateInfo =
        VulkanSwapChainCreateInfo.of(
          this.surface,
          minimumImageCount,
          this.surfaceFormat.format(),
          this.surfaceFormat.colorSpace(),
          this.surfaceExtent,
          1,
          imageUsageFlags,
          imageSharingMode,
          queueIndices,
          this.surfaceCaps.currentTransform(),
          surfaceAlphaFlags,
          this.surfacePresent,
          true,
          Optional.empty()
        );

      this.swapChain =
        this.resourcesPerSwapChain.add(
          this.khrSwapChainExt.swapChainCreate(newDevice, swapChainCreateInfo)
        );

      this.swapChainImages.clear();
      final var images = this.swapChain.images();
      for (int index = 0; index < images.size(); ++index) {
        final var swIndex = new SwapChainIndex(index);
        final var image = images.get(index);
        this.swapChainImages.put(swIndex, image);
        this.swapChainImageViews.put(
          swIndex,
          this.resourcesPerSwapChain.add(this.createImageView(image))
        );
      }

      LOG.debug(
        "Swap chain (actual) image count: {}", this.swapChainImages.size());

      for (int index = 0; index < images.size(); ++index) {
        final var fIndex = new RCFrameIndex(index);

        final var imageReadySemaphore =
          this.device.createSemaphore(
            VulkanSemaphoreCreateInfo.builder()
              .build()
          );

        debugging.setObjectName(
          imageReadySemaphore,
          "Semaphore[ImageReady][%d]".formatted(index)
        );

        this.swapChainImageReadySemaphores.put(
          fIndex,
          this.resourcesPerSwapChain.add(imageReadySemaphore)
        );

        final var renderingDoneSemaphore =
          this.device.createSemaphore(
            VulkanSemaphoreCreateInfo.builder()
              .build()
          );

        debugging.setObjectName(
          renderingDoneSemaphore,
          "Semaphore[RenderingDone][%d]".formatted(index)
        );

        this.swapChainImageRenderingDoneSemaphores.put(
          fIndex,
          this.resourcesPerSwapChain.add(renderingDoneSemaphore)
        );

        /*
         * Create a "rendering done" fence that starts in the signaled state.
         * The reason for starting in the signaled state is that the fence
         * is used to wait before fully acquiring a frame for rendering. If
         * the acquire operation was acquiring a frame for the first time ever,
         * it would wait forever for a fence that will never be signalled.
         */

        final var renderingDoneFence =
          this.device.createFence(
            VulkanFenceCreateInfo.builder()
              .addFlags(VulkanFenceCreateFlag.VK_FENCE_CREATE_SIGNALED_BIT)
              .build()
          );

        debugging.setObjectName(
          renderingDoneFence,
          "Fence[RenderingDone][%d]".formatted(index)
        );

        this.swapChainImageRenderingDoneFences.put(
          fIndex,
          this.resourcesPerSwapChain.add(renderingDoneFence)
        );
      }
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private VulkanImageViewType createImageView(
    final VulkanImageType image)
    throws VulkanException
  {
    final var range =
      VulkanImageSubresourceRange.of(
        Set.of(VK_IMAGE_ASPECT_COLOR_BIT),
        0,
        1,
        0,
        1);

    final Set<VulkanImageViewCreateFlag> flags = Set.of();
    return this.device.createImageView(
      VulkanImageViewCreateInfo.of(
        flags,
        image,
        VK_IMAGE_VIEW_TYPE_2D,
        this.surfaceFormat.format(),
        VulkanComponentMapping.of(
          VK_COMPONENT_SWIZZLE_IDENTITY,
          VK_COMPONENT_SWIZZLE_IDENTITY,
          VK_COMPONENT_SWIZZLE_IDENTITY,
          VK_COMPONENT_SWIZZLE_IDENTITY
        ),
        range
      )
    );
  }

  private RCVulkanException errorMissingRequiredException(
    final String extensionName)
  {
    return new RCVulkanException(
      this.strings.format(ERROR_VULKAN_EXTENSION_MISSING),
      Map.ofEntries(
        Map.entry(this.strings.format(EXTENSION), extensionName)
      ),
      VULKAN_EXTENSION_MISSING.codeName(),
      Optional.empty()
    );
  }

  private VulkanSharingMode pickImageSharingMode(
    final List<VulkanQueueFamilyIndex> queueIndices)
  {
    /*
     * If the graphics and presentation queues are separate families, then
     * add the indices of those families into the given list and enable
     * concurrent sharing mode. Otherwise, don't add any indices, and use
     * exclusive sharing mode.
     */

    final var graphicsFamily =
      this.graphicsQueue.queueFamilyProperties().queueFamilyIndex();
    final var presentationFamily =
      this.presentationQueue.queueFamilyProperties().queueFamilyIndex();

    if (!Objects.equals(graphicsFamily, presentationFamily)) {
      queueIndices.add(graphicsFamily);
      queueIndices.add(presentationFamily);
      return VK_SHARING_MODE_CONCURRENT;
    }
    return VK_SHARING_MODE_EXCLUSIVE;
  }

  private int pickMinimumImageCount()
  {
    final var min =
      this.surfaceCaps.minImageCount();
    final var max =
      this.surfaceCaps.maxImageCount();
    final var minClamped =
      Math.max(min, 3);

    if (max == 0) {
      LOG.debug(
        "Implementation reports no limit on the maximum number of swapchain images.");
      return minClamped;
    }

    return Math.min(minClamped, max);
  }

  /**
   * Work out the extent of the rendered image based on the
   * implementation-defined supported limits.
   */

  private VulkanExtent2D pickExtent()
  {
    LOG.debug("Window size: {}", this.window.size());

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
    final VulkanPhysicalDeviceType physicalDevice)
    throws VulkanException
  {
    final var modes =
      this.khrSurfaceExt.surfacePresentModes(physicalDevice, this.surface);

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
    final VulkanPhysicalDeviceType physicalDevice)
    throws VulkanException
  {
    final var formats =
      this.khrSurfaceExt.surfaceFormats(physicalDevice, this.surface);

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
   * The type of indices that are returned by swap chain image acquisition.
   *
   * @param index The index value
   */

  private record SwapChainIndex(
    int index)
    implements Comparable<SwapChainIndex>
  {
    @Override
    public int compareTo(
      final SwapChainIndex other)
    {
      return Integer.compareUnsigned(this.index, other.index);
    }

    @Override
    public String toString()
    {
      return "[SwapChainIndex %d]".formatted(this.index);
    }
  }
}
