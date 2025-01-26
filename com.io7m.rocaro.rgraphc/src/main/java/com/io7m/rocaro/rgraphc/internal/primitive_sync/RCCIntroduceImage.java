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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderImageType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveProducer;

import java.util.Objects;

/**
 * A command that represents the introduction of an image object.
 */

public final class RCCIntroduceImage
  extends RCCAbstractCommand
  implements RCCIntroduceType
{
  private final RCCExecute owner;
  private final RCCPPlaceholderImageType resource;
  private final RCCPortPrimitiveProducer port;
  private final RCGResourceImageLayout imageLayout;

  /**
   * A read command.
   *
   * @param id            The ID
   * @param inOwner       The executing command
   * @param inResource    The resource
   * @param inPort        The port
   * @param inImageLayout The image layout
   */

  public RCCIntroduceImage(
    final long id,
    final RCCExecute inOwner,
    final RCCPPlaceholderImageType inResource,
    final RCCPortPrimitiveProducer inPort,
    final RCGResourceImageLayout inImageLayout)
  {
    super(id);

    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.resource =
      Objects.requireNonNull(inResource, "resource");
    this.port =
      Objects.requireNonNull(inPort, "port");
    this.imageLayout =
      Objects.requireNonNull(inImageLayout, "imageLayout");
  }

  /**
   * @return The starting image layout
   */

  public RCGResourceImageLayout imageLayout()
  {
    return this.imageLayout;
  }

  @Override
  public RCCPortPrimitiveProducer port()
  {
    return this.port;
  }

  @Override
  public RCCExecute owner()
  {
    return this.owner;
  }

  @Override
  public RCCPPlaceholderImageType resource()
  {
    return this.resource;
  }
}
