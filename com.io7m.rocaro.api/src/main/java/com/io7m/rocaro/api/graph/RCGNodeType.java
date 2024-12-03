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


package com.io7m.rocaro.api.graph;

import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A node in the render graph.
 *
 * @param <P> The type of parameters
 */

public sealed interface RCGNodeType<P>
  permits RCGFrameNodeType, RCGResourceNodeType, RCRenderPassType
{
  /**
   * @return The unique-within-a-graph node name
   */

  RCGNodeName name();

  /**
   * @return The node parameters
   */

  P parameters();

  /**
   * @return The node ports
   */

  Map<RCGPortName, RCGPortType<?>> ports();

  /**
   * @return The node producer ports
   */

  default Map<RCGPortName, RCGPortProducer<?>> portProducers()
  {
    return this.ports()
      .values()
      .stream()
      .filter(p -> p instanceof RCGPortProducer<?>)
      .map(p -> (RCGPortProducer<?>) p)
      .map(p -> Map.entry(p.name(), p))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return The node consumer ports
   */

  default Map<RCGPortName, RCGPortConsumer<?>> portConsumers()
  {
    return this.ports()
      .values()
      .stream()
      .filter(p -> p instanceof RCGPortConsumer<?>)
      .map(p -> (RCGPortConsumer<?>) p)
      .map(p -> Map.entry(p.name(), p))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Prepare the node for evaluation.
   *
   * @param context The context
   *
   * @throws RocaroException On errors
   */

  void prepare(
    RCGNodePreparationContextType context)
    throws RocaroException;

  /**
   * Evaluate the node.
   *
   * @param context The context
   *
   * @throws RocaroException On errors
   */

  void evaluate(RCGNodeRenderContextType context)
    throws RocaroException;
}
