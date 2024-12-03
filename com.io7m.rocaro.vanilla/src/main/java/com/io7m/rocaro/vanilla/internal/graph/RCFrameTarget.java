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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanImageAspectFlag;
import com.io7m.jcoronado.api.VulkanImageLayout;
import com.io7m.jcoronado.api.VulkanImageMemoryBarrier;
import com.io7m.jcoronado.api.VulkanImageSubresourceRange;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGFrameNodeTargetType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodePreparationContextType;
import com.io7m.rocaro.api.graph.RCGNodeRenderContextType;
import com.io7m.rocaro.api.graph.RCGPortConsumer;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.jcoronado.api.VulkanAccessFlag.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static com.io7m.jcoronado.api.VulkanCommandBufferUsageFlag.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static com.io7m.jcoronado.api.VulkanPipelineStageFlag.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

/**
 * The frame target.
 */

public final class RCFrameTarget
  extends RCObject
  implements RCGFrameNodeTargetType
{
  private final RCGNodeName name;
  private final Map<RCGPortName, RCGPortType<?>> ports;
  private final RCGPortConsumer<RCImageColorBlendableType> imageSink;

  /**
   * The frame target.
   *
   * @param inName      The node name
   * @param inPorts     The ports
   * @param inImageSink The image sink port
   */

  public RCFrameTarget(
    final RCGNodeName inName,
    final Map<RCGPortName, RCGPortType<?>> inPorts,
    final RCGPortConsumer<RCImageColorBlendableType> inImageSink)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.ports =
      Map.copyOf(inPorts);
    this.imageSink =
      Objects.requireNonNull(inImageSink, "imageSink");
  }

  @Override
  public RCGNodeName name()
  {
    return this.name;
  }

  @Override
  public RCUnit parameters()
  {
    return RCUnit.UNIT;
  }

  @Override
  public Map<RCGPortName, RCGPortType<?>> ports()
  {
    return this.ports;
  }

  @Override
  public void prepare(
    final RCGNodePreparationContextType context)
  {

  }

  @Override
  public void evaluate(
    final RCGNodeRenderContextType context)
    throws RCVulkanException
  {
    try {
      final var vulkanContext =
        context.frameScopedService(RCVulkanFrameContextType.class);
      final var windowContext =
        vulkanContext.windowFrameContext();
      final var device =
        vulkanContext.device();
      final var vulkanDevice =
        device.device();
      final var debugging =
        vulkanDevice.debugging();

      final var commands =
        vulkanDevice.createCommandBuffer(
          vulkanContext.commandPool(),
          VK_COMMAND_BUFFER_LEVEL_PRIMARY
        );

      debugging.setObjectName(commands, "FrameTargetShowImage");

      try (final var _ =
             debugging.begin(commands, "FramePresentation")) {
        final var imageBarrier =
          VulkanImageMemoryBarrier.builder()
            .setSourceAccessMask(Set.of(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT))
            .setTargetAccessMask(Set.of())
            .setOldLayout(VulkanImageLayout.VK_IMAGE_LAYOUT_UNDEFINED)
            .setNewLayout(VulkanImageLayout.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            .setSourceQueueFamilyIndex(device.graphicsQueue().queueFamilyIndex())
            .setTargetQueueFamilyIndex(device.graphicsQueue().queueFamilyIndex())
            .setImage(windowContext.image().data())
            .setSubresourceRange(VulkanImageSubresourceRange.of(
              Set.of(VulkanImageAspectFlag.VK_IMAGE_ASPECT_COLOR_BIT),
              0,
              1,
              0,
              1
            ))
            .build();

        commands.beginCommandBuffer(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        commands.pipelineBarrier(
          Set.of(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
          Set.of(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT),
          Set.of(),
          List.of(),
          List.of(),
          List.of(imageBarrier)
        );
        commands.endCommandBuffer();
      }

      device.graphicsQueue()
        .submit(List.of(
          VulkanSubmitInfo.builder()
            .addCommandBuffers(commands)
            .addWaitSemaphores(windowContext.imageIsReady())
            .addWaitStageMasks(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .addSignalSemaphores(windowContext.imageRenderingIsFinished())
            .build()
        ), Optional.of(windowContext.imageRenderingIsFinishedFence()));

      windowContext.present();
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }
}
