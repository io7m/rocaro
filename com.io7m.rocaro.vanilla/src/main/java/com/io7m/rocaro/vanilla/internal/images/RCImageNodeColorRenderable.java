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
import com.io7m.rocaro.api.images.RCImageColorRenderableType;
import com.io7m.rocaro.api.images.RCImageConstraintColorRenderable;
import com.io7m.rocaro.api.images.RCImageNodeDescriptionType;
import com.io7m.rocaro.api.images.RCImageNodeType;
import com.io7m.rocaro.api.images.RCImageParametersRenderable;

import java.util.HashMap;
import java.util.Optional;

/**
 * The image renderable node.
 */

public enum RCImageNodeColorRenderable
  implements RCGNodeDescriptionFactoryType<
  RCImageParametersRenderable,
  RCImageNodeType<RCImageParametersRenderable>,
  RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType>>
{
  /**
   * The image renderable node.
   */

  IMAGE_NODE_COLOR_RENDERABLE;

  static final RCGPortName SAMPLE_PORT_NAME =
    new RCGPortName("Samples");

  @Override
  public RCImageNodeDescriptionType<RCImageParametersRenderable, RCImageColorRenderableType> createDescription(
    final RCImageParametersRenderable parameters,
    final RCGNodeName name)
  {
    final var output =
      new RCGPortProducer<>(
        name,
        SAMPLE_PORT_NAME,
        new RCImageConstraintColorRenderable(
          Optional.of(parameters.size()),
          parameters.channels()
        )
      );

    final var ports = new HashMap<RCGPortName, RCGPortType<?>>();
    ports.put(output.name(), output);
    return new RCImageNodeColorRenderableDescription(parameters, name, ports);
  }

  @Override
  public String type()
  {
    return "ImageColorRenderable";
  }
}
