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


package com.io7m.rocaro.vanilla.internal.images;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorRenderableType;
import com.io7m.rocaro.api.images.RCImageNodeDescriptionType;
import com.io7m.rocaro.api.images.RCImageNodeType;
import com.io7m.rocaro.api.images.RCImageParametersRenderable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static com.io7m.rocaro.vanilla.internal.images.RCImageNodeColorRenderable.SAMPLE_PORT_NAME;

record RCImageNodeColorRenderableDescription(
  RCImageParametersRenderable parameters,
  RCGNodeName name,
  Map<RCGPortName, RCGPortType<?>> ports)
  implements RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType>
{
  RCImageNodeColorRenderableDescription
  {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(name, "name");
    ports = Collections.unmodifiableMap(
      Objects.requireNonNull(ports, "ports")
    );
  }

  @Override
  public RCGPortSourceType<RCImageColorRenderableType> mainOutput()
  {
    return (RCGPortSourceType<RCImageColorRenderableType>)
      this.ports.get(SAMPLE_PORT_NAME);
  }

  @Override
  public RCImageNodeType<RCImageParametersRenderable> createNode()
  {
    throw new UnimplementedCodeException();
  }

  @Override
  public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return VulkanPhysicalDeviceFeaturesFunctions.none();
  }
}
