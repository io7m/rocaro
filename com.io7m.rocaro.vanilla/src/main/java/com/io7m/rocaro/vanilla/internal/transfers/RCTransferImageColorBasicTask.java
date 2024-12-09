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


package com.io7m.rocaro.vanilla.internal.transfers;

import com.io7m.jcoronado.api.VulkanBufferCreateInfo;
import com.io7m.jcoronado.api.VulkanBufferImageCopy;
import com.io7m.jcoronado.api.VulkanBufferType;
import com.io7m.jcoronado.api.VulkanCommandBufferSubmitInfo;
import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanComponentMapping;
import com.io7m.jcoronado.api.VulkanComponentSwizzle;
import com.io7m.jcoronado.api.VulkanDebuggingType;
import com.io7m.jcoronado.api.VulkanDependencyInfo;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent3D;
import com.io7m.jcoronado.api.VulkanImageCreateInfo;
import com.io7m.jcoronado.api.VulkanImageKind;
import com.io7m.jcoronado.api.VulkanImageMemoryBarrier;
import com.io7m.jcoronado.api.VulkanImageSubresourceLayers;
import com.io7m.jcoronado.api.VulkanImageSubresourceRange;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewCreateInfo;
import com.io7m.jcoronado.api.VulkanImageViewKind;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanOffset3D;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.api.VulkanSemaphoreSubmitInfo;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.jcoronado.vma.VMAAllocationCreateInfo;
import com.io7m.jcoronado.vma.VMAAllocationResult;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.images.RCImageColorBasicType;
import com.io7m.rocaro.api.transfers.RCTransferImageColorBasicType;
import com.io7m.rocaro.api.transfers.RCTransferJFREventStagingCopy;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.fences.RCFenceServiceType;
import com.io7m.rocaro.vanilla.internal.images.RCImageColorBasic;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;
import com.io7m.rocaro.vanilla.internal.threading.RCThreadLabels;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.io7m.jcoronado.api.VulkanAccessFlag.VK_ACCESS_SHADER_READ_BIT;
import static com.io7m.jcoronado.api.VulkanAccessFlag.VK_ACCESS_TRANSFER_WRITE_BIT;
import static com.io7m.jcoronado.api.VulkanBufferUsageFlag.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static com.io7m.jcoronado.api.VulkanCommandBufferUsageFlag.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static com.io7m.jcoronado.api.VulkanImageAspectFlag.VK_IMAGE_ASPECT_COLOR_BIT;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_UNDEFINED;
import static com.io7m.jcoronado.api.VulkanImageTiling.VK_IMAGE_TILING_OPTIMAL;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_SAMPLED_BIT;
import static com.io7m.jcoronado.api.VulkanImageUsageFlag.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static com.io7m.jcoronado.api.VulkanMemoryPropertyFlag.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static com.io7m.jcoronado.api.VulkanMemoryPropertyFlag.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_COPY_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
import static com.io7m.jcoronado.api.VulkanSampleCountFlag.VK_SAMPLE_COUNT_1_BIT;
import static com.io7m.jcoronado.api.VulkanSharingMode.VK_SHARING_MODE_EXCLUSIVE;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_CPU_ONLY;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_GPU_ONLY;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.TRANSFER;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.TRANSFER_IO;

final class RCTransferImageColorBasicTask
  extends RCObject
  implements RCTransferTaskType<RCImageColorBasicType>
{
  private final RCDeviceType device;
  private final VMAAllocatorType allocator;
  private final CloseableCollectionType<RocaroException> resources;
  private final RCTransferImageColorBasicType image2D;
  private final VulkanQueueType transferQueue;
  private final VulkanQueueType targetQueue;
  private final RCFenceServiceType fences;
  private final VulkanLogicalDeviceType vulkanDevice;
  private final VulkanImageSubresourceRange imageSubresourceRange;
  private final RCTransferCommandBufferFactoryType commandBuffers;
  private final VulkanDebuggingType debugging;
  private VulkanCommandBufferType mainCommands;

  RCTransferImageColorBasicTask(
    final RCDeviceType inDevice,
    final VMAAllocatorType inAllocator,
    final RCTransferCommandBufferFactoryType inCommandBuffers,
    final RCStrings strings,
    final RCFenceServiceType inFences,
    final RCTransferImageColorBasicType inImage2D)
  {
    Objects.requireNonNull(strings, "strings");

    this.resources =
      RCResourceCollections.create(strings);
    this.fences =
      Objects.requireNonNull(inFences, "fences");
    this.device =
      Objects.requireNonNull(inDevice, "device");
    this.allocator =
      Objects.requireNonNull(inAllocator, "allocator");
    this.commandBuffers =
      Objects.requireNonNull(inCommandBuffers, "commandBuffers");
    this.image2D =
      Objects.requireNonNull(inImage2D, "image2D");

    this.transferQueue =
      this.device.transferQueue();
    this.targetQueue =
      inDevice.queueForCategory(this.image2D.targetQueue());
    this.vulkanDevice =
      inDevice.device();
    this.debugging =
      this.vulkanDevice.debugging();

    this.imageSubresourceRange =
      VulkanImageSubresourceRange.builder()
        .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
        .setBaseMipLevel(0)
        .setBaseArrayLayer(0)
        .setLevelCount(1)
        .setLayerCount(1)
        .build();
  }

  private VMAAllocationResult<VulkanImageType> createGPUTexture()
    throws VulkanException
  {
    final var width =
      this.image2D.size().x();
    final var height =
      this.image2D.size().y();
    final var format =
      this.image2D.format();

    final var textureCreateInfo =
      VulkanImageCreateInfo.builder()
        .setArrayLayers(1)
        .setExtent(VulkanExtent3D.of(width, height, 1))
        .setFormat(format)
        .setImageType(VulkanImageKind.VK_IMAGE_TYPE_2D)
        .setInitialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        .setMipLevels(1)
        .setSamples(Set.of(VK_SAMPLE_COUNT_1_BIT))
        .setSharingMode(VK_SHARING_MODE_EXCLUSIVE)
        .setTiling(VK_IMAGE_TILING_OPTIMAL)
        .addUsage(VK_IMAGE_USAGE_SAMPLED_BIT)
        .addUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT)
        .build();

    final var allocInfo =
      VMAAllocationCreateInfo.builder()
        .setUsage(VMA_MEMORY_USAGE_GPU_ONLY)
        .addRequiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        .setMemoryTypeBits(0L)
        .build();

    final var image =
      this.allocator.createImage(allocInfo, textureCreateInfo);

    this.debugging.setObjectName(
      image.result(),
      "Image[%s]".formatted(this.image2D.name())
    );
    return image;
  }

  @RCThread(TRANSFER_IO)
  private VMAAllocationResult<VulkanBufferType> createCPUStagingBuffer(
    final CloseableCollectionType<RocaroException> taskResources)
    throws VulkanException
  {
    RCThreadLabels.checkThreadLabelsAny(TRANSFER_IO);

    final var width =
      this.image2D.size().x();
    final var height =
      this.image2D.size().y();
    final var format =
      this.image2D.format();

    final var size =
      Integer.toUnsignedLong(width)
      * Integer.toUnsignedLong(height)
      * Integer.toUnsignedLong(format.texelSizeOctets());

    final var ev = new RCTransferJFREventStagingCopy();
    ev.begin();

    final var allocInfo =
      VMAAllocationCreateInfo.builder()
        .setUsage(VMA_MEMORY_USAGE_CPU_ONLY)
        .addRequiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
        .setMemoryTypeBits(0L)
        .build();

    final var stagingBufferCreateInfo =
      VulkanBufferCreateInfo.builder()
        .setSize(size)
        .addUsageFlags(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
        .setSharingMode(VK_SHARING_MODE_EXCLUSIVE)
        .build();

    final var stagingBuffer =
      this.allocator.createBuffer(allocInfo, stagingBufferCreateInfo);

    this.debugging.setObjectName(
      stagingBuffer.result(),
      "TransferStagingBuffer"
    );

    taskResources.add(stagingBuffer.result());

    try (final var map =
           this.allocator.mapMemory(stagingBuffer.allocation())) {
      final var target =
        MemorySegment.ofBuffer(map.asByteBuffer());
      this.image2D.dataCopier().copy(target);
      map.flush();
    }

    if (ev.shouldCommit()) {
      ev.message = "Copying to a CPU-side staging buffer.";
      ev.transferID = this.image2D.id().toString();
      ev.size = size;
      ev.commit();
    }

    return stagingBuffer;
  }

  @Override
  public RCImageColorBasicType execute()
    throws Exception
  {
    RCThreadLabels.checkThreadLabelsAll(TRANSFER_IO);

    try {
      this.mainCommands =
        this.commandBuffers.commandBufferForQueue(
          this.resources,
          this.device.transferQueue(),
          "MainTransferCommands"
        );

      final var stagingR =
        this.createCPUStagingBuffer(this.resources);
      final var imageR =
        this.createGPUTexture();
      final var transferFuture =
        this.scheduleTransferCommands(imageR, stagingR);

      transferFuture.get(5L, TimeUnit.SECONDS);

      final var imageView =
        this.createImageView(imageR.result());

      return new RCImageColorBasic(
        this.image2D.size(),
        imageR.result(),
        imageView,
        this.image2D.format()
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private VulkanImageViewType createImageView(
    final VulkanImageType image)
    throws VulkanException
  {
    final var imageViewInfo =
      VulkanImageViewCreateInfo.builder()
        .setImage(image)
        .setViewType(VulkanImageViewKind.VK_IMAGE_VIEW_TYPE_2D)
        .setFormat(this.image2D.format())
        .setComponents(VulkanComponentMapping.of(
          VulkanComponentSwizzle.VK_COMPONENT_SWIZZLE_IDENTITY,
          VulkanComponentSwizzle.VK_COMPONENT_SWIZZLE_IDENTITY,
          VulkanComponentSwizzle.VK_COMPONENT_SWIZZLE_IDENTITY,
          VulkanComponentSwizzle.VK_COMPONENT_SWIZZLE_IDENTITY
        ))
        .setSubresourceRange(this.imageSubresourceRange)
        .build();

    final var imageView =
      this.vulkanDevice.createImageView(imageViewInfo);

    this.debugging.setObjectName(
      imageView,
      "ImageView[%s]".formatted(this.image2D.name())
    );
    return imageView;
  }

  @RCThread(TRANSFER_IO)
  private CompletableFuture<?> scheduleTransferCommands(
    final VMAAllocationResult<VulkanImageType> gpuImageResult,
    final VMAAllocationResult<VulkanBufferType> stagingBufferResult)
    throws VulkanException, RocaroException
  {
    /*
     * The operations we perform are slightly different depending on whether
     * the transfer needs to happen on a single queue or multiple.
     */

    if (Objects.equals(this.transferQueue, this.targetQueue)) {
      return this.scheduleTransferCommandsSingleQueue(
        gpuImageResult,
        stagingBufferResult
      );
    } else {
      return this.scheduleTransferCommandsMultiQueue(
        gpuImageResult,
        stagingBufferResult
      );
    }
  }

  @RCThread(TRANSFER_IO)
  private CompletableFuture<?> scheduleTransferCommandsMultiQueue(
    final VMAAllocationResult<VulkanImageType> gpuImageResult,
    final VMAAllocationResult<VulkanBufferType> stagingBufferResult)
    throws VulkanException, RocaroException
  {
    final var width =
      this.image2D.size().x();
    final var height =
      this.image2D.size().y();
    final var finalLayout =
      this.image2D.finalLayout();

    final var otherCommands =
      this.commandBuffers.commandBufferForQueue(
        this.resources,
        this.targetQueue,
        "OwnershipTransferCommands"
      );

    /*
     * Create a semaphore that will be used to execute the commands on
     * the graphics queue after the commands on the transfer queue.
     */

    final var semaphore =
      this.resources.add(this.vulkanDevice.createBinarySemaphore());
    this.debugging.setObjectName(semaphore, "TransferSemaphore");

    /*
     * Create a fence that will be used to determine when the entire image
     * transfer is completed.
     */

    final var fence =
      this.resources.add(this.vulkanDevice.createFence());
    this.debugging.setObjectName(fence, "TransferFence[Transfer]");

    /*
     * Record the commands that will be executed by the transfer queue.
     * This is, essentially, "copy the image data and transition it to the
     * final layout".
     */

    try (final var _ =
           this.debugging.begin(this.mainCommands, "TransferQueueUpload")) {

      this.mainCommands.beginCommandBuffer(
        VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
      );

      /*
       * Transition the image into a state that is optimal for being the
       * destination of a transfer operation.
       */

      {
        final var preCopyTransitionBarrier =
          VulkanImageMemoryBarrier.builder()
            .setSrcStageMask(Set.of())
            .setSrcAccessMask(Set.of())
            .setSrcQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
            .setDstStageMask(Set.of(VK_PIPELINE_STAGE_COPY_BIT))
            .setDstAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
            .setDstQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
            .setImage(gpuImageResult.result())
            .setOldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .setNewLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            .setSubresourceRange(this.imageSubresourceRange)
            .build();

        this.mainCommands.pipelineBarrier(
          VulkanDependencyInfo.builder()
            .addImageMemoryBarriers(preCopyTransitionBarrier)
            .build()
        );
      }

      /*
       * Copy the contents of the CPU-side staging buffer into the image.
       */

      final var bufferImageCopy =
        VulkanBufferImageCopy.builder()
          .setBufferImageHeight(0)
          .setBufferOffset(0L)
          .setBufferRowLength(0)
          .setImageExtent(VulkanExtent3D.of(width, height, 1))
          .setImageOffset(VulkanOffset3D.of(0, 0, 0))
          .setImageSubresource(
            VulkanImageSubresourceLayers.builder()
              .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
              .setBaseArrayLayer(0)
              .setLayerCount(1)
              .setMipLevel(0)
              .build())
          .build();

      this.mainCommands.copyBufferToImage(
        stagingBufferResult.result(),
        gpuImageResult.result(),
        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        List.of(bufferImageCopy)
      );

      /*
       * Transition the now-populated image into the specified final layout.
       * Because we're working with two different queues, we need to execute
       * a barrier on both queues.
       */

      {
        final var postCopyTransitionBarrier0 =
          VulkanImageMemoryBarrier.builder()
            .setSrcStageMask(Set.of(VK_PIPELINE_STAGE_COPY_BIT))
            .setSrcAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
            .setSrcQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
            .setDstStageMask(Set.of())
            .setDstAccessMask(Set.of())
            .setDstQueueFamilyIndex(this.targetQueue.queueFamilyIndex())
            .setImage(gpuImageResult.result())
            .setOldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            .setNewLayout(finalLayout)
            .setSubresourceRange(this.imageSubresourceRange)
            .build();

        this.mainCommands.pipelineBarrier(
          VulkanDependencyInfo.builder()
            .addImageMemoryBarriers(postCopyTransitionBarrier0)
            .build()
        );
      }

      this.mainCommands.endCommandBuffer();
    }

    /*
     * Put together the work submission for the transfer queue. The work
     * will signal the given semaphore when it completes.
     */

    final var transferSemaphoreSubmission =
      VulkanSemaphoreSubmitInfo.builder()
        .addStageMask(VK_PIPELINE_STAGE_COPY_BIT)
        .setSemaphore(semaphore)
        .build();

    final var transferCommandSubmission =
      VulkanCommandBufferSubmitInfo.builder()
        .setCommandBuffer(this.mainCommands)
        .build();

    final var transferSubmission =
      VulkanSubmitInfo.builder()
        .addCommandBuffers(transferCommandSubmission)
        .addSignalSemaphores(transferSemaphoreSubmission)
        .build();

    /*
     * Record the commands that will be executed by the graphics queue.
     * This is merely one half of an ownership transfer.
     */

    try (final var _ =
           this.debugging.begin(this.mainCommands, "GraphicsQueueUpload")) {

      final var postCopyTransitionBarrier1 =
        VulkanImageMemoryBarrier.builder()
          .setSrcStageMask(Set.of(VK_PIPELINE_STAGE_COPY_BIT))
          .setSrcAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setSrcQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setDstStageMask(Set.of(VK_PIPELINE_STAGE_VERTEX_SHADER_BIT))
          .setDstAccessMask(Set.of(VK_ACCESS_SHADER_READ_BIT))
          .setDstQueueFamilyIndex(this.targetQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setOldLayout(finalLayout)
          .setNewLayout(finalLayout)
          .build();

      otherCommands.beginCommandBuffer(
        VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
      );
      otherCommands.pipelineBarrier(
        VulkanDependencyInfo.builder()
          .addImageMemoryBarriers(postCopyTransitionBarrier1)
          .build()
      );
      otherCommands.endCommandBuffer();
    }

    /*
     * Put together the work submission for the graphics queue. The work
     * will wait on the transfer semaphore before executing, and will signal
     * the given fence when completed.
     */

    final var graphicsCommandSubmission =
      VulkanCommandBufferSubmitInfo.builder()
        .setCommandBuffer(otherCommands)
        .build();

    final var graphicsSemaphoreSubmission =
      VulkanSemaphoreSubmitInfo.builder()
        .addStageMask(VK_PIPELINE_STAGE_COPY_BIT)
        .setSemaphore(semaphore)
        .build();

    final var graphicsSubmission =
      VulkanSubmitInfo.builder()
        .addCommandBuffers(graphicsCommandSubmission)
        .addWaitSemaphores(graphicsSemaphoreSubmission)
        .build();

    this.device.submit(
      TRANSFER,
      List.of(transferSubmission),
      Optional.empty()
    );
    this.device.submit(
      GRAPHICS,
      List.of(graphicsSubmission),
      Optional.of(fence)
    );

    return this.fences.registerTransferFence(fence);
  }

  @RCThread(TRANSFER_IO)
  private CompletableFuture<?> scheduleTransferCommandsSingleQueue(
    final VMAAllocationResult<VulkanImageType> gpuImageResult,
    final VMAAllocationResult<VulkanBufferType> stagingBufferResult)
    throws VulkanException
  {
    final var width =
      this.image2D.size().x();
    final var height =
      this.image2D.size().y();
    final var finalLayout =
      this.image2D.finalLayout();

    /*
     * Create a fence that will be used to determine when the entire image
     * transfer is completed. This fence will ultimately be monitored by
     * the fence service.
     */

    final var fence =
      this.resources.add(this.vulkanDevice.createFence());
    this.debugging.setObjectName(fence, "TransferFence");

    try (final var _ =
           this.debugging.begin(this.mainCommands, "TransferQueueUpload")) {

      this.mainCommands.beginCommandBuffer(
        VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
      );

      /*
       * Transition the image into a state that is optimal for being the
       * destination of a transfer operation.
       */

      final var preCopyTransitionBarrier =
        VulkanImageMemoryBarrier.builder()
          .setSrcStageMask(Set.of())
          .setSrcAccessMask(Set.of())
          .setSrcQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
          .setDstStageMask(Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT))
          .setDstAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setDstQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setOldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .setNewLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .build();

      this.mainCommands.pipelineBarrier(
        VulkanDependencyInfo.builder()
          .addImageMemoryBarriers(preCopyTransitionBarrier)
          .build()
      );

      /*
       * Copy the contents of the CPU-side staging buffer into the image.
       */

      final var bufferImageCopy =
        VulkanBufferImageCopy.builder()
          .setBufferImageHeight(0)
          .setBufferOffset(0L)
          .setBufferRowLength(0)
          .setImageExtent(VulkanExtent3D.of(width, height, 1))
          .setImageOffset(VulkanOffset3D.of(0, 0, 0))
          .setImageSubresource(
            VulkanImageSubresourceLayers.builder()
              .addAspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
              .setBaseArrayLayer(0)
              .setLayerCount(1)
              .setMipLevel(0)
              .build())
          .build();

      this.mainCommands.copyBufferToImage(
        stagingBufferResult.result(),
        gpuImageResult.result(),
        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        List.of(bufferImageCopy)
      );

      /*
       * Transition the now-populated image into the specified final layout.
       * Because we're only working with a single queue, we only need to
       * use a single pipeline barrier on this queue.
       */

      final var postCopyTransitionBarrier =
        VulkanImageMemoryBarrier.builder()
          .setSrcStageMask(Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT))
          .setSrcAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setSrcQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
          .setDstStageMask(Set.of(VK_PIPELINE_STAGE_VERTEX_SHADER_BIT))
          .setDstAccessMask(Set.of(VK_ACCESS_SHADER_READ_BIT))
          .setDstQueueFamilyIndex(VulkanQueueFamilyIndex.ignored())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setOldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .setNewLayout(finalLayout)
          .build();

      this.mainCommands.pipelineBarrier(
        VulkanDependencyInfo.builder()
          .addImageMemoryBarriers(postCopyTransitionBarrier)
          .build()
      );

      this.mainCommands.endCommandBuffer();
    }

    final var commandSubmission =
      VulkanCommandBufferSubmitInfo.builder()
        .setCommandBuffer(this.mainCommands)
        .build();

    final var submission =
      VulkanSubmitInfo.builder()
        .addCommandBuffers(commandSubmission)
        .build();

    this.device.submit(
      this.targetQueue,
      List.of(submission),
      Optional.of(fence)
    );

    return this.fences.registerTransferFence(fence);
  }

  @RCThread(GPU)
  @Override
  public void close()
    throws RocaroException
  {
    RCThreadLabels.checkThreadLabelsAny(GPU);
    this.resources.close();
  }
}
