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

import com.io7m.rocaro.api.graph2.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationParametersType;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortModifies;
import com.io7m.rocaro.api.graph2.RCGPortName;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph2.RCGResourceImageType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class OpImageTransition0
  implements RCGOperationType
{
  private final RCGOperationName name;
  private final Parameters parameters;

  public OpImageTransition0(
    final RCGOperationName inName,
    final Parameters inParameters)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");
  }

  public static RCGOperationFactoryType<Parameters, OpImageTransition0> factory()
  {
    return OpImageTransition0::new;
  }

  record Parameters(
    Optional<RCGResourceImageLayout> requiresLayout,
    Optional<RCGResourceImageLayout> ensuresLayout)
    implements RCGOperationParametersType
  {
    Parameters
    {
      Objects.requireNonNull(requiresLayout, "requiresLayout");
      Objects.requireNonNull(ensuresLayout, "ensuresLayout");
    }
  }

  public RCGPortModifies port0()
  {
    return new RCGPortModifies(
      this,
      new RCGPortName("Port0"),
      RCGResourceImageType.class,
      Set.of(),
      Set.of(),
      this.parameters.requiresLayout(),
      this.parameters.ensuresLayout()
    );
  }

  @Override
  public RCGOperationName name()
  {
    return this.name;
  }

  @Override
  public List<RCGPortType> ports()
  {
    return List.of(this.port0());
  }
}
