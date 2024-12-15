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


package com.io7m.rocaro.api.graph2;

/**
 * A compiled render graph.
 */

public interface RCGGraphType
{
  /**
   * Obtain the resource for the given port.
   *
   * @param port The port
   *
   * @return The resource
   */

  RCGResourceType resourceAt(
    RCGPortType port);

  /**
   * Obtain the image layout transition (possibly just a constant value)
   * at the given port. The function returns a constant {@link RCGResourceImageLayout#LAYOUT_UNDEFINED}
   * value for all non-image ports.
   *
   * @param port The port
   *
   * @return The layout transition
   */

  RCGOperationImageLayoutTransitionType imageTransitionAt(
    RCGPortType port);
}
