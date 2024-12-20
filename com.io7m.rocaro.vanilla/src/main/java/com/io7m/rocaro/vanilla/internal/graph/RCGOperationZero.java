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

import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.graph.RCGNoParameters;
import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGOperationZeroType;
import com.io7m.rocaro.api.graph.RCGPortModifies;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderRenderTargetType;
import com.io7m.rocaro.api.render_targets.RCRenderTargetType;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;
import static com.io7m.rocaro.api.graph.RCGCommandPipelineStage.STAGE_TRANSFER_CLEAR;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET;

public final class RCGOperationZero
  extends RCGOperationAbstract
  implements RCGOperationZeroType
{
  private final RCGPortModifies frame;

  public RCGOperationZero(
    final RCGOperationName inName)
  {
    super(inName, GRAPHICS);

    this.frame =
      this.addPort(
        new RCGPortModifies(
          this,
          new RCGPortName("Frame"),
          RCGResourcePlaceholderRenderTargetType.class,
          Set.of(),
          Set.of(STAGE_TRANSFER_CLEAR),
          Optional.of(LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET),
          Optional.empty()
        )
      );
  }

  public static RCGOperationFactoryType<RCGNoParameters, RCGOperationZeroType> factory()
  {
    return (name, _) -> new RCGOperationZero(name);
  }

  @Override
  protected void onExecute(
    final RCGOperationExecutionContextType context)
    throws RocaroException
  {
    final var frameImage =
      context.portRead(this.frame, RCRenderTargetType.class);
  }

  @Override
  protected void onPrepare(
    final RCGOperationPreparationContextType context)
    throws RocaroException
  {

  }

  @Override
  protected void onPrepareCheck(
    final RCGOperationPreparationContextType context)
    throws RocaroException
  {

  }

  @Override
  public RCGPortModifies frame()
  {
    return this.frame;
  }
}
