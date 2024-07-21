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

import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodeType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * A fully instantiated render graph.
 */

public final class RCGraph
{
  private final RCGraphDescription description;
  private final Map<RCGNodeName, RCGNodeType<?>> nodes;
  private final HashSet<RCGNodeName> nodesEvaluated;

  RCGraph(
    final RCGraphDescription inDescription,
    final Map<RCGNodeName, RCGNodeType<?>> inNodes)
  {
    this.description =
      Objects.requireNonNull(inDescription, "description");
    this.nodes =
      Map.copyOf(inNodes);
    this.nodesEvaluated =
      new HashSet<>(this.nodes.size());
  }

  @Override
  public String toString()
  {
    return "[RCGraph %s '%s']".formatted(
      Integer.toUnsignedString(this.hashCode(), 16),
      this.description.name()
    );
  }

  /**
   * Evaluate the render graph.
   *
   * @param frameInformation The frame information
   * @param frameContext     The frame context
   *
   * @throws RocaroException On errors
   */

  public void evaluate(
    final RCFrameInformation frameInformation,
    final RCVulkanFrameContextType frameContext)
    throws RocaroException
  {
    final var context =
      new RCGNodeRenderContext(
        this.description.graph(),
        frameInformation,
        frameContext
      );

    this.nodesEvaluated.clear();

    final var nodeGraph =
      this.description.graph();
    final var iterator =
      new TopologicalOrderIterator<>(nodeGraph);

    while (iterator.hasNext()) {
      final var next = iterator.next();
      final var nodeName = next.owner();
      if (this.nodesEvaluated.contains(nodeName)) {
        continue;
      }

      final var node =
        this.nodes.get(nodeName);
      final var producers =
        node.portProducers();

      node.evaluate(context);

      for (final var producer : producers.values()) {
        if (!context.portIsWritten(producer)) {
          throw new IllegalStateException();
        }
      }

      this.nodesEvaluated.add(nodeName);
    }
  }
}
