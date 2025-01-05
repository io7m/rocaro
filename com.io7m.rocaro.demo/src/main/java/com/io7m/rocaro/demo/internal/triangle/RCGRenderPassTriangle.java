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


package com.io7m.rocaro.demo.internal.triangle;

import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationCreationContextType;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCNoParameters;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.render_targets.RCRenderTargetType;
import com.io7m.rocaro.api.resources.RCDepthComponents;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;
import com.io7m.rocaro.api.resources.RCResourceSchematicRenderTargetType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintImage2D;
import com.io7m.rocaro.api.resources.RCSchematicConstraintRenderTarget;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_ATTACHMENT;

/**
 * A simple triangle render pass.
 */

public final class RCGRenderPassTriangle
  extends RCGOperationAbstract
{
  private final RCGPortModifierType<RCRenderTargetType> frame;

  /**
   * A simple triangle render pass.
   *
   * @param context The context
   * @param inName  The name
   */

  public RCGRenderPassTriangle(
    final
    RCGOperationCreationContextType context,
    final RCGOperationName inName)
  {
    super(inName, GRAPHICS);

    this.frame =
      this.addPort(
        context.createModifierPort(
          this,
          new RCGPortName("Frame"),
          Set.of(),
          new RCSchematicConstraintRenderTarget<>(
            RCRenderTargetType.class,
            RCResourceSchematicRenderTargetType.class,
            List.of(
              new RCSchematicConstraintImage2D<>(
                RCImage2DType.class,
                RCResourceSchematicImage2DType.class,
                Optional.of(LAYOUT_OPTIMAL_FOR_ATTACHMENT),
                false
              )
            ),
            Optional.empty()
          ),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          new RCSchematicConstraintRenderTarget<>(
            RCRenderTargetType.class,
            RCResourceSchematicRenderTargetType.class,
            List.of(
              new RCSchematicConstraintImage2D<>(
                RCImage2DType.class,
                RCResourceSchematicImage2DType.class,
                Optional.empty(),
                false
              )
            ),
            Optional.empty()
          )
        )
      );
  }

  /**
   * @return The frame port
   */

  public RCGPortModifierType<RCRenderTargetType> frame()
  {
    return this.frame;
  }

  /**
   * @return A simple triangle render pass.
   */

  public static RCGOperationFactoryType<RCNoParameters, RCGRenderPassTriangle> factory()
  {
    return (context, name, _) -> new RCGRenderPassTriangle(context, name);
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

  }
}
