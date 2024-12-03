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
import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanComponentMapping;
import com.io7m.jcoronado.api.VulkanComponentSwizzle;
import com.io7m.jcoronado.api.VulkanDebuggingType;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent3D;
import com.io7m.jcoronado.api.VulkanFenceCreateInfo;
import com.io7m.jcoronado.api.VulkanFenceType;
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
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jcoronado.vma.VMAAllocationCreateInfo;
import com.io7m.jcoronado.vma.VMAAllocationResult;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.images.RCImageColorBasicType;
import com.io7m.rocaro.api.transfers.RCTransferImageColorBasicType;
import com.io7m.rocaro.api.transfers.RCTransferJFREventStagingCopy;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.fences.RCFenceServiceType;
import com.io7m.rocaro.vanilla.internal.images.RCImageColorBasic;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutors;
import com.io7m.rocaro.vanilla.internal.threading.RCThread;
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
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
import static com.io7m.jcoronado.api.VulkanSampleCountFlag.VK_SAMPLE_COUNT_1_BIT;
import static com.io7m.jcoronado.api.VulkanSharingMode.VK_SHARING_MODE_EXCLUSIVE;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_CPU_ONLY;
import static com.io7m.jcoronado.vma.VMAMemoryUsage.VMA_MEMORY_USAGE_GPU_ONLY;
import static com.io7m.rocaro.api.RCUnit.UNIT;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU_COMPUTE;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU_GRAPHICS;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU_TRANSFER;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.TRANSFER_IO;

final class RCTransferImageColorBasicTask
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

  @RCThread(GPU_TRANSFER)
  private volatile VulkanCommandBufferType mainCommands;
  private VulkanCommandBufferType otherCommands;

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
      this.image2D.targetQueue();
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
    RCExecutors.checkThreadLabelsAny(TRANSFER_IO);

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
      final var transferFences =
        this.scheduleTransferCommands(imageR, stagingR);

      transferFences.future.get(5L, TimeUnit.SECONDS);

      final var imageView =
        RCExecutors.executeAndWait(
          this.device.executorForQueue(this.image2D.targetQueue()),
          () -> this.createImageView(imageR.result())
        );

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

  @RCThread({GPU_COMPUTE, GPU_GRAPHICS, GPU_TRANSFER})
  private VulkanImageViewType createImageView(
    final VulkanImageType image)
    throws VulkanException
  {
    RCExecutors.checkThreadLabelsAny(GPU_COMPUTE, GPU_GRAPHICS, GPU_TRANSFER);

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

  private record Fences(
    VulkanFenceType sourceFence,
    Optional<VulkanFenceType> targetFence,
    CompletableFuture<?> future)
  {

  }

  private Fences scheduleTransferCommands(
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

  private Fences scheduleTransferCommandsMultiQueue(
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

    this.otherCommands =
      this.commandBuffers.commandBufferForQueue(
        this.resources,
        this.targetQueue,
        "OwnershipTransferCommands"
      );

    /*
     * Create two fences that will be used to determine when the entire image
     * transfer is completed. The fences will ultimately be monitored by
     * the fence service. We need two fences because we have to make submissions
     * to two different queues.
     */

    final var fenceInfo =
      VulkanFenceCreateInfo.builder()
        .build();

    final var fence0 =
      this.vulkanDevice.createFence(fenceInfo);

    this.debugging.setObjectName(fence0, "TransferFence[Transfer]");

    this.resources.add(() -> {
      RCExecutors.executeAndWait(
        this.device.transferExecutor(),
        () -> {
          fence0.close();
          return UNIT;
        });
    });

    final var fence1 =
      this.vulkanDevice.createFence(fenceInfo);

    this.debugging.setObjectName(fence0, "TransferFence[Target]");

    this.resources.add(() -> {
      RCExecutors.executeAndWait(
        this.device.executorForQueue(this.targetQueue),
        () -> {
          fence1.close();
          return UNIT;
        });
    });

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
          .setSourceQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setTargetQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setSourceAccessMask(Set.of())
          .setTargetAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setOldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .setNewLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .build();

      this.mainCommands.pipelineBarrier(
        Set.of(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
        Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT),
        Set.of(),
        List.of(),
        List.of(),
        List.of(preCopyTransitionBarrier)
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
       * Because we're working with two different queues, we need to execute
       * a barrier on both queues.
       */

      final var postCopyTransitionBarrier0 =
        VulkanImageMemoryBarrier.builder()
          .setSourceQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setTargetQueueFamilyIndex(this.targetQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setSourceAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setTargetAccessMask(Set.of())
          .setOldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .setNewLayout(finalLayout)
          .build();

      this.mainCommands.pipelineBarrier(
        Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT),
        Set.of(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT),
        Set.of(),
        List.of(),
        List.of(),
        List.of(postCopyTransitionBarrier0)
      );
      this.mainCommands.endCommandBuffer();
    }

    try (final var _ =
           this.debugging.begin(this.mainCommands, "GraphicsQueueUpload")) {

      final var postCopyTransitionBarrier1 =
        VulkanImageMemoryBarrier.builder()
          .setSourceQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setTargetQueueFamilyIndex(this.targetQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setSourceAccessMask(Set.of())
          .setTargetAccessMask(Set.of(VK_ACCESS_SHADER_READ_BIT))
          .setOldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .setNewLayout(finalLayout)
          .build();

      this.otherCommands.beginCommandBuffer(
        VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
      );
      this.otherCommands.pipelineBarrier(
        Set.of(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
        Set.of(VK_PIPELINE_STAGE_VERTEX_SHADER_BIT),
        Set.of(),
        List.of(),
        List.of(),
        List.of(postCopyTransitionBarrier1)
      );
      this.otherCommands.endCommandBuffer();
    }

    this.device.transferSubmitWithFence(
      fence0,
      this.mainCommands
    );
    this.device.submitWithFence(
      this.device.categoryForQueue(this.targetQueue),
      fence1,
      this.otherCommands
    );

    final var future0 =
      this.fences.registerTransferFence(fence0);

    final var future1 =
      switch (this.device.categoryForQueue(this.targetQueue)) {
        case COMPUTE -> this.fences.registerComputeFence(fence1);
        case GRAPHICS -> this.fences.registerGraphicsFence(fence1);
        case TRANSFER -> this.fences.registerTransferFence(fence1);
      };

    return new Fences(
      fence0,
      Optional.of(fence1),
      CompletableFuture.allOf(future0, future1)
    );
  }

  private Fences scheduleTransferCommandsSingleQueue(
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

    /*
     * Create a fence that will be used to determine when the entire image
     * transfer is completed. This fence will ultimately be monitored by
     * the fence service.
     */

    final var fenceInfo =
      VulkanFenceCreateInfo.builder()
        .build();

    final var fence =
      this.vulkanDevice.createFence(fenceInfo);

    this.resources.add(() -> {
      RCExecutors.executeAndWait(this.device.transferExecutor(), () -> {
        fence.close();
        return UNIT;
      });
    });

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
          .setSourceQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setTargetQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setSourceAccessMask(Set.of())
          .setTargetAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setOldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
          .setNewLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .build();

      this.mainCommands.pipelineBarrier(
        Set.of(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
        Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT),
        Set.of(),
        List.of(),
        List.of(),
        List.of(preCopyTransitionBarrier)
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
          .setSourceQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setTargetQueueFamilyIndex(this.transferQueue.queueFamilyIndex())
          .setImage(gpuImageResult.result())
          .setSubresourceRange(this.imageSubresourceRange)
          .setSourceAccessMask(Set.of(VK_ACCESS_TRANSFER_WRITE_BIT))
          .setTargetAccessMask(Set.of(VK_ACCESS_SHADER_READ_BIT))
          .setOldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
          .setNewLayout(finalLayout)
          .build();

      this.mainCommands.pipelineBarrier(
        Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT),
        Set.of(VK_PIPELINE_STAGE_TRANSFER_BIT),
        Set.of(),
        List.of(),
        List.of(),
        List.of(postCopyTransitionBarrier)
      );

      this.mainCommands.endCommandBuffer();
    }

    this.device.transferSubmitWithFence(fence, this.mainCommands);

    return new Fences(
      fence,
      Optional.empty(),
      this.fences.registerTransferFence(fence)
    );
  }

  @Override
  public void close()
    throws RocaroException
  {
    this.resources.close();
  }
}
