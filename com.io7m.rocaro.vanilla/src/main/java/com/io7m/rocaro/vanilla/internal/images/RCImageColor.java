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
import com.io7m.rocaro.api.images.RCImageColorConstraint;
import com.io7m.rocaro.api.images.RCImageColorFormat;
import com.io7m.rocaro.api.images.RCImageDescriptionType;
import com.io7m.rocaro.api.images.RCImageNodeType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A color image.
 */

public enum RCImageColor implements RCGNodeFactoryType<
  RCImageColorFormat,
  RCImageNodeType<RCImageColorFormat>,
  RCImageDescriptionType<RCImageColorFormat>>
{
  /**
   * A color image.
   */

  IMAGE_COLOR;

  private static final RCGPortName SAMPLE_PORT_NAME =
    new RCGPortName("Samples");

  @Override
  public RCImageDescriptionType<RCImageColorFormat> createDescription(
    final RCImageColorFormat parameters,
    final RCGNodeName name)
  {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(name, "name");

    final var output =
      new RCGPortProducer<>(
        name,
        SAMPLE_PORT_NAME,
        RCImageColorConstraint.exactColorFormat(parameters)
      );

    final var ports = new HashMap<RCGPortName, RCGPortType<?>>();
    ports.put(output.name(), output);
    return new Description(parameters, name, ports);
  }

  @Override
  public RCImageNodeType<RCImageColorFormat> create(
    final RCImageDescriptionType<RCImageColorFormat> description)
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
    RCImageColorFormat parameters,
    RCGNodeName name,
    Map<RCGPortName, RCGPortType<?>> ports)
    implements RCImageDescriptionType<RCImageColorFormat>
  {
    Description
    {
      Objects.requireNonNull(parameters, "parameters");
      Objects.requireNonNull(name, "name");
      ports = Collections.unmodifiableMap(
        Objects.requireNonNull(ports, "ports")
      );
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
