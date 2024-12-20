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

import com.io7m.rocaro.api.graph.RCGNoParameters;
import com.io7m.rocaro.api.graph.RCGOperationAbstract;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationPreparationContextType;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortConsumes;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProduces;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderBufferType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderImageType;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;

final class OpEx1
  extends RCGOperationAbstract
  implements RCGOperationType
{
  private final RCGPortProduces port0;
  private final RCGPortConsumes port1;

  public OpEx1(
    final RCGOperationName inName)
  {
    super(inName, GRAPHICS);

    this.port0 =
      new RCGPortProduces(
        this,
        new RCGPortName("Port0"),
        RCGResourcePlaceholderBufferType.class,
        Set.of(),
        Set.of(),
        Optional.empty()
      );

    this.port1 =
      new RCGPortConsumes(
        this,
        new RCGPortName("Port1"),
        RCGResourcePlaceholderImageType.class,
        Set.of(),
        Set.of(),
        Optional.empty()
      );

    this.addPort(this.port0);
    this.addPort(this.port1);
  }

  public static RCGOperationFactoryType<RCGNoParameters, OpEx1> factory()
  {
    return (name, _) -> new OpEx1(name);
  }

  public RCGPortProduces port0()
  {
    return this.port0;
  }

  public RCGPortConsumes port1()
  {
    return this.port1;
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
}
