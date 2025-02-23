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

import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortModifier;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.api.images.RCImageColorChannels;
import com.io7m.rocaro.api.images.RCImageConstraintColorBlendable;
import com.io7m.rocaro.api.images.RCImageSizeWindowFraction;
import com.io7m.rocaro.api.render_pass.RCRenderPassDescriptionType;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;

import java.util.Map;
import java.util.Optional;

final class RCRenderPassTriangleDescription
  implements RCRenderPassDescriptionType<RCUnit, RCRenderPassType<RCUnit>>
{
  private final RCUnit parameters;
  private final RCGNodeName name;
  private final RCGPortModifier<RCImageColorBlendableType> mainImagePort;
  private final Map<RCGPortName, RCGPortType<?>> ports;

  RCRenderPassTriangleDescription(
    final RCUnit inParameters,
    final RCGNodeName inName)
  {
    this.parameters =
      inParameters;
    this.name =
      inName;
    this.mainImagePort =
      new RCGPortModifier<>(
        this.name,
        new RCGPortName("Image"),
        new RCImageConstraintColorBlendable(
          Optional.of(new RCImageSizeWindowFraction(1.0)),
          RCImageColorChannels.RGBA
        )
      );

    this.ports =
      Map.ofEntries(Map.entry(this.mainImagePort.name(), this.mainImagePort));
  }

  @Override
  public Map<RCGPortName, RCGPortType<?>> ports()
  {
    return this.ports;
  }

  @Override
  public RCRenderPassType<RCUnit> createNode()
  {
    return new RcRenderPassTriangle(
      this.name,
      RCUnit.UNIT,
      this.ports
    );
  }

  @Override
  public RCUnit parameters()
  {
    return this.parameters;
  }

  @Override
  public RCGNodeName name()
  {
    return this.name;
  }
}
