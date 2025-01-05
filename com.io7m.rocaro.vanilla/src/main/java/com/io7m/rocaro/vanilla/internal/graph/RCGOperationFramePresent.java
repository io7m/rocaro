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

import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationCreationContextType;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFramePresentType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.render_targets.RCRenderTargetType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameType;

import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGOperationStatusType.Ready.READY;
import static com.io7m.rocaro.vanilla.internal.graph.RCGOperationFrameAcquire.PRESENTATION_RENDER_TARGET_CONSTRAINT;

/**
 * The frame presentation operation.
 */

public final class RCGOperationFramePresent
  extends RCGOperationAbstract
  implements RCGOperationFramePresentType
{
  private final RCGPortConsumerType<RCRenderTargetType> frame;

  /**
   * The frame presentation operation.
   *
   * @param context The creation context
   * @param inName  The name
   */

  public RCGOperationFramePresent(
    final RCGOperationCreationContextType context,
    final RCGOperationName inName)
  {
    /*
     * Technically, this operation occurs on the presentation queue, but
     * the underlying abstractions in the renderer allow us to treat this
     * as if it was a graphics queue operation (in other words, no explicit
     * image transfer required).
     */

    super(inName, GRAPHICS);

    this.frame =
      this.addPort(
        context.createConsumerPort(
          this,
          new RCGPortName("Frame"),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          PRESENTATION_RENDER_TARGET_CONSTRAINT,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        )
      );

    this.setStatus(READY);
  }

  @Override
  public RCGPortConsumerType<RCRenderTargetType> frame()
  {
    return this.frame;
  }

  @Override
  protected void onPrepare(
    final RCGOperationPreparationContextType context)
  {

  }

  @Override
  protected void onPrepareContinue(
    final RCGOperationPreparationContextType context)
  {

  }

  @Override
  protected void onExecute(
    final RCGOperationExecutionContextType context)
    throws RCVulkanException
  {
    final var vulkan =
      context.frameScopedService(RCVulkanFrameType.class);
    final var windowContext =
      vulkan.windowFrameContext();
    final var device =
      vulkan.device();

    device.execute(() -> {
      windowContext.present();
      return RCUnit.UNIT;
    });
  }
}
