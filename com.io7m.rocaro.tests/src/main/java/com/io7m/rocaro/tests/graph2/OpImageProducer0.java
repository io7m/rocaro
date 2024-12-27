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
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintImage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderImageType;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class OpImageProducer0
  extends RCGOperationAbstract
  implements RCGOperationType
{
  private final Parameters parameters;
  private final RCGPortProducerType port;

  public OpImageProducer0(
    final RCGOperationCreationContextType context,
    final RCGOperationName inName,
    final Parameters inParameters)
  {
    super(inName, inParameters.queueCategory());

    this.parameters =
      Objects.requireNonNull(inParameters, "p");

    this.port =
      context.createProducerPort(
        this,
        new RCGPortName("Port0"),
        this.parameters.readsOnStages(),
        new RCGPortTypeConstraintImage<>(
          RCGResourcePlaceholderImageType.class,
          this.parameters.ensuresLayout()
        ),
        this.parameters.writesOnStages()
      );

    this.addPort(this.port);
  }

  public static RCGOperationFactoryType<Parameters, OpImageProducer0> factory()
  {
    return OpImageProducer0::new;
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

  public RCGPortProducerType port()
  {
    return this.port;
  }

  record Parameters(
    RCDeviceQueueCategory queueCategory,
    Set<RCGCommandPipelineStage> readsOnStages,
    Set<RCGCommandPipelineStage> writesOnStages,
    Optional<RCGResourceImageLayout> ensuresLayout)
    implements RCGOperationParametersType
  {

  }
}
