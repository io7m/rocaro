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

import com.io7m.rocaro.api.graph.RCGNodeDescriptionFactoryType;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducer;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.images.RCImageConstraintDepthStencil;
import com.io7m.rocaro.api.images.RCImageDepthStencilType;
import com.io7m.rocaro.api.images.RCImageNodeDescriptionType;
import com.io7m.rocaro.api.images.RCImageNodeType;
import com.io7m.rocaro.api.images.RCImageParametersDepthStencil;

import java.util.HashMap;
import java.util.Optional;

/**
 * The image depth+stencil node.
 */

public enum RCImageNodeDepthStencil
  implements RCGNodeDescriptionFactoryType<
  RCImageParametersDepthStencil,
  RCImageNodeType<RCImageParametersDepthStencil>,
  RCImageNodeDescriptionType<RCImageParametersDepthStencil, RCImageDepthStencilType>>
{
  /**
   * The image depth+stencil node.
   */

  IMAGE_NODE_DEPTH_STENCIL;

  static final RCGPortName SAMPLE_PORT_NAME =
    new RCGPortName("Samples");

  @Override
  public RCImageNodeDescriptionType<RCImageParametersDepthStencil, RCImageDepthStencilType> createDescription(
    final RCImageParametersDepthStencil parameters,
    final RCGNodeName name)
  {
    final var output =
      new RCGPortProducer<>(
        name,
        SAMPLE_PORT_NAME,
        new RCImageConstraintDepthStencil(Optional.of(parameters.size()))
      );

    final var ports = new HashMap<RCGPortName, RCGPortType<?>>();
    ports.put(output.name(), output);
    return new RCImageNodeDepthStencilDescription(parameters, name, ports);
  }

  @Override
  public String type()
  {
    return "ImageColorDepthStencil";
  }
}
