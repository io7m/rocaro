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


package com.io7m.rocaro.tests;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.render_pass.RCRenderPassDescriptionType;
import com.io7m.rocaro.api.render_pass.RCRenderPassDescriptionFactoryType;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class RCFakeRenderPass
  implements RCRenderPassDescriptionFactoryType<
    Integer,
    RCRenderPassType<Integer>,
    RCRenderPassDescriptionType<Integer, RCRenderPassType<Integer>>>
{
  private final VulkanPhysicalDeviceFeatures features;
  private final List<Function<RCGNodeName, RCGPortType<?>>> ports;

  public RCFakeRenderPass(
    final VulkanPhysicalDeviceFeatures inFeatures,
    final List<Function<RCGNodeName, RCGPortType<?>>> inPorts)
  {
    this.features =
      Objects.requireNonNull(inFeatures, "features");
    this.ports =
      Objects.requireNonNull(inPorts, "ports");
  }

  @SafeVarargs
  public static RCFakeRenderPass of(
    final VulkanPhysicalDeviceFeatures features,
    final Function<RCGNodeName, RCGPortType<?>>... ports)
  {
    return new RCFakeRenderPass(features, List.of(ports));
  }

  @Override
  public RCRenderPassDescriptionType<Integer, RCRenderPassType<Integer>> createDescription(
    final Integer parameters,
    final RCGNodeName name)
  {
    final var portMap =
      new HashMap<RCGPortName, RCGPortType<?>>();
    final var description =
      new Description(parameters, name, portMap, this.features);

    for (final var portConstructor : this.ports) {
      final var port = portConstructor.apply(name);
      portMap.put(port.name(), port);
    }

    return description;
  }

  @Override
  public String type()
  {
    return "Render Pass";
  }

  private record Description(
    Integer parameters,
    RCGNodeName name,
    Map<RCGPortName, RCGPortType<?>> ports,
    VulkanPhysicalDeviceFeatures requiredDeviceFeatures)
    implements RCRenderPassDescriptionType<Integer, RCRenderPassType<Integer>>
  {

    @Override
    public RCRenderPassType<Integer> createNode()
    {
      throw new UnimplementedCodeException();
    }
  }
}
