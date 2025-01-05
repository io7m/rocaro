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

import com.io7m.rocaro.api.buffers.RCBufferType;
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
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.resources.RCResourceSchematicBufferType;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintBuffer;

import java.util.Objects;
import java.util.Set;

final class OpProducer3
  extends RCGOperationAbstract
  implements RCGOperationType
{
  private final Parameters parameters;
  private final RCGPortProducerType<RCBufferType> port0;
  private final RCGPortProducerType<RCBufferType> port1;
  private final RCGPortProducerType<RCBufferType> port2;

  public OpProducer3(
    final RCGOperationCreationContextType context,
    final RCGOperationName inName,
    final Parameters inParameters)
  {
    super(inName, inParameters.queueCategory());

    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");

    this.port0 =
      context.createProducerPort(
        this,
        new RCGPortName("Port0"),
        this.parameters.reads(),
        new RCSchematicConstraintBuffer<>(
          RCBufferType.class,
          RCResourceSchematicBufferType.class
        ),
        this.parameters.writes()
      );
    this.port1 =
      context.createProducerPort(
        this,
        new RCGPortName("Port1"),
        this.parameters.reads(),
        new RCSchematicConstraintBuffer<>(
          RCBufferType.class,
          RCResourceSchematicBufferType.class
        ),
        this.parameters.writes()
      );
    this.port2 =
      context.createProducerPort(
        this,
        new RCGPortName("Port2"),
        this.parameters.reads(),
        new RCSchematicConstraintBuffer<>(
          RCBufferType.class,
          RCResourceSchematicBufferType.class
        ),
        this.parameters.writes()
      );

    this.addPort(this.port0);
    this.addPort(this.port1);
    this.addPort(this.port2);
  }

  public static RCGOperationFactoryType<Parameters, OpProducer3> factory()
  {
    return OpProducer3::new;
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

  public RCGPortProducerType<RCBufferType> port0()
  {
    return this.port0;
  }

  public RCGPortProducerType<RCBufferType> port1()
  {
    return this.port1;
  }

  public RCGPortProducerType<RCBufferType> port2()
  {
    return this.port2;
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
