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
import com.io7m.rocaro.api.graph.RCGNodeFactoryType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducer;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageDepthConstraint;
import com.io7m.rocaro.api.images.RCImageDepthFormatType;
import com.io7m.rocaro.api.images.RCImageDescriptionType;
import com.io7m.rocaro.api.images.RCImageNodeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A depth image.
 */

public enum RCImageDepth implements RCGNodeFactoryType<
  RCImageDepthFormatType,
  RCImageNodeType<RCImageDepthFormatType>,
  RCImageDescriptionType<RCImageDepthFormatType>>
{
  /**
   * A depth image.
   */

  IMAGE_DEPTH;

  private static final RCGPortName SAMPLE_PORT_NAME =
    new RCGPortName("Samples");

  @Override
  public RCImageDescriptionType<RCImageDepthFormatType> createDescription(
    final RCImageDepthFormatType parameters,
    final RCGNodeName name)
  {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(name, "name");

    final var output =
      new RCGPortProducer<>(
        name,
        SAMPLE_PORT_NAME,
        RCImageDepthConstraint.exactDepth(parameters)
      );

    final var ports = new HashMap<RCGPortName, RCGPortType<?>>();
    ports.put(output.name(), output);
    return new Description(parameters, name, ports);
  }

  @Override
  public RCImageNodeType<RCImageDepthFormatType> create(
    final RCImageDescriptionType<RCImageDepthFormatType> description)
  {
    Objects.requireNonNull(description, "description");
    throw new UnimplementedCodeException();
  }

  @Override
  public String type()
  {
    return "Image";
  }

  private record Description(
    RCImageDepthFormatType parameters,
    RCGNodeName name,
    Map<RCGPortName, RCGPortType<?>> ports)
    implements RCImageDescriptionType<RCImageDepthFormatType>
  {
    Description
    {
      Objects.requireNonNull(parameters, "parameters");
      Objects.requireNonNull(name, "name");
      ports = Map.copyOf(ports);
    }

    @Override
    public RCGPortSourceType<?> mainOutput()
    {
      return (RCGPortSourceType<?>) this.ports.get(SAMPLE_PORT_NAME);
    }

    @Override
    public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
    {
      return VulkanPhysicalDeviceFeaturesFunctions.none();
    }
  }
}
