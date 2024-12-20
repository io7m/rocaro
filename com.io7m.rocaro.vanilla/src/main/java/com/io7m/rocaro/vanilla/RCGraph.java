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


package com.io7m.rocaro.vanilla;

import com.io7m.rocaro.api.graph.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.internal.graph.RCGGraphBuilder;

/**
 * The default render graph implementation.
 */

public final class RCGraph
{
  private RCGraph()
  {

  }

  /**
   * Create a new render graph builder.
   *
   * @param name The name of the graph
   *
   * @return The builder
   */

  public static RCGGraphBuilderType builder(
    final RCGraphName name)
  {
    return new RCGGraphBuilder(name);
  }

  /**
   * Create a new render graph builder.
   *
   * @param name The name of the graph
   *
   * @return The builder
   */

  public static RCGGraphBuilderType builder(
    final String name)
  {
    return builder(new RCGraphName(name));
  }
}
