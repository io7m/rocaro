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


package com.io7m.rocaro.vanilla.internal.renderpass.empty;

import com.io7m.jcoronado.api.VulkanClearValueColorFloatingPoint;
import com.io7m.jcoronado.api.VulkanClearValueDepthStencil;
import com.io7m.jcoronado.api.VulkanCommandBufferSubmitInfo;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtent2D;
import com.io7m.jcoronado.api.VulkanOffset2D;
import com.io7m.jcoronado.api.VulkanRectangle2D;
import com.io7m.jcoronado.api.VulkanRenderPassBeginInfo;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodePreparationContextType;
import com.io7m.rocaro.api.graph.RCGNodeRenderContextType;
import com.io7m.rocaro.api.graph.RCGNodeRenderPassAbstract;
import com.io7m.rocaro.api.graph.RCGPortModifier;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;
import com.io7m.rocaro.api.render_targets.RCRenderTargetType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static com.io7m.jcoronado.api.VulkanCommandBufferUsageFlag.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static com.io7m.jcoronado.api.VulkanSubpassContents.VK_SUBPASS_CONTENTS_INLINE;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;

final class RCRenderPassEmpty
  extends RCGNodeRenderPassAbstract<RCUnit>
  implements RCRenderPassType<RCUnit>
{
  private static final VulkanClearValueColorFloatingPoint GREY =
    VulkanClearValueColorFloatingPoint.of(0.5f, 0.5f, 0.5f, 1.0f);

  private static final VulkanClearValueDepthStencil EMPTY_DEPTH_STENCIL =
    VulkanClearValueDepthStencil.of(0.0f, 0);

  private final RCGPortModifier<RCImageColorBlendableType> imagePort;

  RCRenderPassEmpty(
    final RCGNodeName inName,
    final Map<RCGPortName, RCGPortType<?>> inPorts,
    final RCGPortModifier<RCImageColorBlendableType> inImagePort)
  {
    super(inName, RCUnit.UNIT, inPorts);
    this.imagePort = Objects.requireNonNull(inImagePort, "imagePort");
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
      final var image =
        context.portRead(this.imagePort);
      final var vulkanContext =
        context.frameScopedService(RCVulkanFrameContextType.class);
      final RCRenderTargetType renderTarget =
        context.renderTargetFor(image);
      final var device =
        vulkanContext.device();
      final var vkDevice =
        device.device();

      final var debugging =
        vkDevice.debugging();

      final var cmds =
        vkDevice.createCommandBuffer(
          vulkanContext.commandPool(),
          VK_COMMAND_BUFFER_LEVEL_PRIMARY
        );

      try (final var _ = debugging.begin(cmds, "Empty")) {
        cmds.beginCommandBuffer(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        cmds.beginRenderPass(
          VulkanRenderPassBeginInfo.builder()
            .setRenderPass(renderTarget.renderPass())
            .setRenderArea(VulkanRectangle2D.of(
              VulkanOffset2D.of(0, 0),
              VulkanExtent2D.of(image.size().x(), image.size().y())
            ))
            .addClearValues(GREY)
            .addClearValues(EMPTY_DEPTH_STENCIL)
            .setFramebuffer(renderTarget.frameBuffer())
            .build(),
          VK_SUBPASS_CONTENTS_INLINE
        );
        cmds.endRenderPass();
        cmds.endCommandBuffer();
      }

      device.submit(
        GRAPHICS,
        List.of(
          VulkanSubmitInfo.builder()
            .addCommandBuffers(
              VulkanCommandBufferSubmitInfo.builder()
                .setCommandBuffer(cmds)
                .build()
            )
            .build()
        ),
        Optional.empty()
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }
}
