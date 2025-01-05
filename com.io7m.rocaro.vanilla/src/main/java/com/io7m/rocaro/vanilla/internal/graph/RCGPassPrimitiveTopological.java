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

import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPassPortPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveType;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sort primitive ports into topological order.
 */

public final class RCGPassPrimitiveTopological
  extends RCGPassAbstract
  implements RCGGraphPassType
{
  /**
   * Sort primitive ports into topological order.
   */

  public RCGPassPrimitiveTopological()
  {
    super(Set.of(RCGPassPortPrimitive.class));
  }

  @Override
  public void process(
    final RCGGraphBuilderInternalType builder)
    throws RCGGraphException
  {
    this.buildPortTopological(builder);
  }

  private void buildPortTopological(
    final RCGGraphBuilderInternalType builder)
  {
    final var iter =
      new TopologicalOrderIterator<>(builder.primitivePortGraph());
    final var r =
      new ArrayList<RCGPortPrimitiveType>(
        builder.graph().vertexSet().size()
      );

    while (iter.hasNext()) {
      r.add(iter.next());
    }

    builder.setPrimitivePortsOrdered(List.copyOf(r));
  }
}
