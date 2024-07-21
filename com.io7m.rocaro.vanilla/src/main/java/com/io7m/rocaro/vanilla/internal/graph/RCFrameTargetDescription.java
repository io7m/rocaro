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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGFrameNodeTargetDescriptionType;
import com.io7m.rocaro.api.graph.RCGFrameNodeTargetType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortConsumer;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;
import com.io7m.rocaro.api.images.RCImageColorChannels;
import com.io7m.rocaro.api.images.RCImageConstraintColorBlendable;
import com.io7m.rocaro.api.images.RCImageSizeWindowFraction;
import com.io7m.rocaro.vanilla.internal.RCObject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The frame target.
 */

public final class RCFrameTargetDescription
  extends RCObject
  implements RCGFrameNodeTargetDescriptionType
{
  private final RCGNodeName name;
  private final RCGPortConsumer<RCImageColorBlendableType> imageSink;

  /**
   * The frame target.
   *
   * @param inName The node name
   */

  public RCFrameTargetDescription(
    final RCGNodeName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");

    this.imageSink =
      new RCGPortConsumer<>(
        this.name,
        new RCGPortName("Image"),
        new RCImageConstraintColorBlendable(
          Optional.of(new RCImageSizeWindowFraction(1.0)),
          RCImageColorChannels.RGBA
        )
      );
  }

  @Override
  public RCGNodeName name()
  {
    return this.name;
  }

  @Override
  public Map<RCGPortName, RCGPortType<?>> ports()
  {
    return Map.of(this.imageSink.name(), this.imageSink);
  }

  @Override
  public VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return VulkanPhysicalDeviceFeaturesFunctions.none();
  }

  @Override
  public RCGFrameNodeTargetType createNode()
  {
    return new RCFrameTarget(
      this.name,
      this.ports(),
      this.imageSink
    );
  }

  @Override
  public RCUnit parameters()
  {
    return RCUnit.UNIT;
  }

  @Override
  public RCGPortConsumer<RCImageColorBlendableType> imageTarget()
  {
    return this.imageSink;
  }
}
