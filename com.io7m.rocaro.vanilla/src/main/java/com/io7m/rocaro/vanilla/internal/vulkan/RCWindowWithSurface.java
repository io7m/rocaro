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
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSemaphoreBinaryType;
import com.io7m.jcoronado.api.VulkanSemaphoreType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_surface.api.VulkanExtKHRSurfaceType.VulkanKHRSurfaceType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanExtKHRSwapChainType.VulkanKHRSwapChainType;
import com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentInfoKHR;
import com.io7m.jcoronado.utility.swapchain.JCSwapchainConfiguration;
import com.io7m.jcoronado.utility.swapchain.JCSwapchainManager;
import com.io7m.jcoronado.utility.swapchain.JCSwapchainManagerType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.images.RCImageID;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetType;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;
import com.io7m.rocaro.vanilla.internal.threading.RCThreadLabels;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanCompositeAlphaFlagKHR.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
import static com.io7m.jcoronado.extensions.khr_swapchain.api.VulkanPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_EXTENSION_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.EXTENSION;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;

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
  private final CloseableCollectionType<RocaroException> resources;
  private final CloseableCollectionType<RocaroException> resourcesPerSwapChain;
  private VulkanQueueType presentationQueue;
  private RCDeviceType device;
  private VulkanExtKHRSwapChainType khrSwapChainExt;
  private VulkanKHRSwapChainType swapChain;
  private VulkanLogicalDeviceType vkDevice;
  private JCSwapchainManagerType swapChainManager;
  private final ByteBuffer idBytes;
  private final byte[] idBytesArray;

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

    this.resources =
      RCResourceCollections.create(this.strings);
    this.resourcesPerSwapChain =
      RCResourceCollections.create(this.strings);
    this.closed =
      new AtomicBoolean(false);

    this.resources.add(this.surface);
    this.resources.add(this.window);

    this.idBytesArray =
      new byte[4];
    this.idBytes =
      ByteBuffer.wrap(this.idBytesArray);
  }

  @RCThread(GPU)
  @Override
  public void close()
    throws RocaroException
  {
    RCThreadLabels.checkThreadLabelsAny(GPU);

    if (this.closed.compareAndSet(false, true)) {
      this.device.waitUntilIdle();

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
    private final RCFrameRenderTarget image;
    private final RCWindowWithSurface windowWithSurface;
    private final SwapChainIndex index;
    private final VulkanFenceType imageRenderingIsFinishedFence;
    private final VulkanSemaphoreBinaryType imageIsReadySemaphore;
    private final VulkanSemaphoreBinaryType imageRenderingIsFinishedSemaphore;

    private FrameContext(
      final RCWindowWithSurface inWindowWithSurface,
      final SwapChainIndex inIndex,
      final VulkanSemaphoreBinaryType inImageIsReadySemaphore,
      final VulkanSemaphoreBinaryType inImageRenderingIsFinishedSemaphore,
      final VulkanFenceType inImageRenderingIsFinishedFence,
      final RCFrameImage inImage)
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
        new RCFrameRenderTarget(
          inImage,
          new RCFrameRenderTargetSchematic(inImage.schematic())
        );
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
    public RCPresentationRenderTargetType image()
    {
      return this.image;
    }

    @Override
    @RCThread(GPU)
    public void present()
      throws RCVulkanException
    {
      RCThreadLabels.checkThreadLabelsAny(GPU);

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
  }

  @Override
  public RCWindowFrameContextType acquireFrame(
    final RCFrameIndex frameIndex,
    final Duration timeout)
    throws RCVulkanException
  {
    Objects.requireNonNull(frameIndex, "frameIndex");
    Objects.requireNonNull(timeout, "timeout");

    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Acquiring frame {}", frameIndex);
      }

      final var image =
        this.swapChainManager.acquire();

      final var frameImage =
        new RCFrameImage(
          this.frameImageID(frameIndex),
          image.image(),
          image.imageView(),
          new RCFrameImageSchematic(
            Vector2I.of(
              (int) image.size().width(),
              (int) image.size().height()
            ),
            image.format()
          )
        );

      return new FrameContext(
        this,
        new SwapChainIndex(image.index().value()),
        image.imageReadySemaphore(),
        image.renderFinishedSemaphore(),
        image.renderFinishedFence(),
        frameImage
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private RCImageID frameImageID(
    final RCFrameIndex frameIndex)
  {
    this.idBytes.putInt(0, frameIndex.value());
    return new RCImageID(UUID.nameUUIDFromBytes(this.idBytesArray));
  }

  @Override
  public int maximumFramesInFlight()
  {
    return this.swapChainManager.imageIndices().size();
  }

  @Override
  public RCWindowType window()
  {
    return this.window;
  }

  @Override
  public void configureForPhysicalDevice(
    final VulkanPhysicalDeviceType newPhysicalDevice)
  {

  }

  @Override
  public void configureForLogicalDevice(
    final RCDeviceType newDevice,
    final VulkanQueueType newGraphicsQueue,
    final VulkanQueueType newPresentationQueue)
    throws RocaroException
  {
    this.device =
      Objects.requireNonNull(newDevice, "device");

    Objects.requireNonNull(newGraphicsQueue, "graphicsQueue");

    this.presentationQueue =
      Objects.requireNonNull(newPresentationQueue, "presentationQueue");
    this.vkDevice =
      this.device.device();

    try {
      this.khrSwapChainExt =
        this.vkDevice.findEnabledExtension(
            "VK_KHR_swapchain", VulkanExtKHRSwapChainType.class)
          .orElseThrow(() -> {
            return this.errorMissingRequiredException("VK_KHR_swapchain");
          });

      final var configuration =
        JCSwapchainConfiguration.builder()
          .setRequestedMinimumImages(2)
          .addPreferredModes(VK_PRESENT_MODE_FIFO_KHR)
          .addPreferredModes(VK_PRESENT_MODE_MAILBOX_KHR)
          .addImageUsageFlags(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
          .addImageUsageFlags(VK_IMAGE_USAGE_TRANSFER_DST_BIT)
          .addSurfaceAlphaFlags(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
          .setDevice(this.vkDevice)
          .setGraphicsQueue(newGraphicsQueue)
          .setPresentationQueue(newPresentationQueue)
          .setSurface(this.surface)
          .setSurfaceExtension(this.khrSurfaceExt)
          .setSwapChainExtension(this.khrSwapChainExt)
          .build();

      this.swapChainManager =
        this.resources.add(JCSwapchainManager.create(configuration));

    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
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
