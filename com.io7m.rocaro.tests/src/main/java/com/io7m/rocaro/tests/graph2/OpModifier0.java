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


package com.io7m.rocaro.tests.graph2;

import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationCreationContextType;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationParametersType;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintBuffer;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderBufferType;

import java.util.Objects;
import java.util.Set;

final class OpModifier0
  extends RCGOperationAbstract
  implements RCGOperationType
{
  private final Parameters parameters;
  private final RCGPortModifierType port;

  public OpModifier0(
    final RCGOperationCreationContextType context,
    final RCGOperationName inName,
    final Parameters inParameters)
  {
    super(inName, inParameters.queueCategory());

    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");

    this.port =
      context.createModifierPort(
        this,
        new RCGPortName("Port0"),
        this.parameters.reads(),
        new RCGPortTypeConstraintBuffer<>(RCGResourcePlaceholderBufferType.class),
        this.parameters.writes(),
        new RCGPortTypeConstraintBuffer<>(RCGResourcePlaceholderBufferType.class)
      );

    this.addPort(this.port);
  }

  public static RCGOperationFactoryType<Parameters, OpModifier0> factory()
  {
    return OpModifier0::new;
  }

  @Override
  protected void onPrepare(
    final RCGOperationPreparationContextType context)
  {

  }

  @Override
  protected void onPrepareCheck(
    final RCGOperationPreparationContextType context)
  {

  }

  @Override
  protected void onExecute(
    final RCGOperationExecutionContextType context)
  {

  }

  public RCGPortModifierType port()
  {
    return this.port;
  }

  record Parameters(
    RCDeviceQueueCategory queueCategory,
    Set<RCGCommandPipelineStage> reads,
    Set<RCGCommandPipelineStage> writes)
    implements RCGOperationParametersType
  {
    Parameters
    {
      reads = Set.copyOf(reads);
      writes = Set.copyOf(writes);
    }
  }
}
