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

import com.io7m.rocaro.api.graph2.RCGNoParameters;
import com.io7m.rocaro.api.graph2.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortConsumes;
import com.io7m.rocaro.api.graph2.RCGPortName;
import com.io7m.rocaro.api.graph2.RCGPortProduces;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceBufferType;
import com.io7m.rocaro.api.graph2.RCGResourceImageType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class OpEx1
  implements RCGOperationType
{
  private final RCGOperationName name;

  public OpEx1(
    final RCGOperationName inName)
  {
    this.name = Objects.requireNonNull(inName, "name");
  }

  public static RCGOperationFactoryType<RCGNoParameters, OpEx1> factory()
  {
    return (name, _) -> new OpEx1(name);
  }

  public RCGPortProduces port0()
  {
    return new RCGPortProduces(
      this,
      new RCGPortName("Port0"),
      RCGResourceBufferType.class,
      Set.of(),
      Set.of(),
      Optional.empty()
    );
  }

  public RCGPortConsumes port1()
  {
    return new RCGPortConsumes(
      this,
      new RCGPortName("Port1"),
      RCGResourceImageType.class,
      Set.of(),
      Set.of(),
      Optional.empty()
    );
  }

  @Override
  public String toString()
  {
    return "[OpEx1 %s]".formatted(this.name);
  }

  @Override
  public List<RCGPortType> ports()
  {
    return List.of(this.port0(), this.port1());
  }

  @Override
  public RCGOperationName name()
  {
    return this.name;
  }
}
