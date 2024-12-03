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


package com.io7m.rocaro.api.graph;

import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;

import java.util.Map;
import java.util.Objects;

/**
 * A convenient base class for render pass nodes.
 *
 * @param <P> The type of parameters
 */

public abstract class RCGNodeRenderPassAbstract<P>
  extends RCObject
  implements RCRenderPassType<P>
{
  private final P parameters;
  private final RCGNodeName name;
  private final Map<RCGPortName, RCGPortType<?>> ports;

  protected RCGNodeRenderPassAbstract(
    final RCGNodeName inName,
    final P inParameters,
    final Map<RCGPortName, RCGPortType<?>> inPorts)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");
    this.ports =
      Map.copyOf(inPorts);
  }

  @Override
  public final Map<RCGPortName, RCGPortType<?>> ports()
  {
    return this.ports;
  }

  @Override
  public final RCGNodeName name()
  {
    return this.name;
  }

  @Override
  public final P parameters()
  {
    return this.parameters;
  }
}
