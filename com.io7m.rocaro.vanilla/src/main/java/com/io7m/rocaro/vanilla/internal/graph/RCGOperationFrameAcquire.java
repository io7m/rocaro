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

import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationCreationContextType;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFrameAcquireType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetSchematicType;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetType;
import com.io7m.rocaro.api.resources.RCDepthComponents;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintImage2D;
import com.io7m.rocaro.api.resources.RCSchematicConstraintRenderTarget;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGOperationStatusType.Ready.READY;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;

/**
 * The frame acquisition operation.
 */

public final class RCGOperationFrameAcquire
  extends RCGOperationAbstract
  implements RCGOperationFrameAcquireType
{
  /**
   * The constraint on presentation render targets.
   */

  static final RCSchematicConstraintRenderTarget<
    RCPresentationRenderTargetType,
    RCPresentationRenderTargetSchematicType>
    PRESENTATION_RENDER_TARGET_CONSTRAINT =
    new RCSchematicConstraintRenderTarget<>(
      RCPresentationRenderTargetType.class,
      RCPresentationRenderTargetSchematicType.class,
      List.of(
        new RCSchematicConstraintImage2D<>(
          RCImage2DType.class,
          RCResourceSchematicImage2DType.class,
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION),
          true
        )
      ),
      Optional.empty()
    );

  private final RCGPortProducerType<RCPresentationRenderTargetType> frame;

  /**
   * The frame acquisition operation.
   *
   * @param context The creation context
   * @param inName  The name
   */

  public RCGOperationFrameAcquire(
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
        context.createProducerPort(
          this,
          new RCGPortName("Frame"),
          Set.of(),
          PRESENTATION_RENDER_TARGET_CONSTRAINT,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT)
        )
      );

    this.setStatus(READY);
  }

  @Override
  public RCGPortProducerType<RCPresentationRenderTargetType> frame()
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
  {
    final var vulkan =
      context.frameScopedService(RCVulkanFrameType.class);
    final var windowContext =
      vulkan.windowFrameContext();

    context.portWrite(
      this.frame,
      windowContext.image()
    );
  }
}
