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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.graph.RCGFrameScopedServiceType;
import com.io7m.rocaro.api.graph.RCGNodeRenderContextType;
import com.io7m.rocaro.api.graph.RCGPortConnection;
import com.io7m.rocaro.api.graph.RCGPortConsumer;
import com.io7m.rocaro.api.graph.RCGPortModifier;
import com.io7m.rocaro.api.graph.RCGPortProducer;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Objects;

/**
 * A node render context.
 */

public final class RCGNodeRenderContext
  implements RCGNodeRenderContextType
{
  private final RCFrameInformation frameInformation;
  private final HashMap<RCGPortProducer<?>, Object> portValues;
  private final HashMap<Class<? extends RCGFrameScopedServiceType>, RCGFrameScopedServiceType> frameServices;
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> graph;

  RCGNodeRenderContext(
    final DirectedAcyclicGraph<RCGPortType<?>, RCGPortConnection> inGraph,
    final RCFrameInformation inFrameInformation,
    final RCVulkanFrameContextType inFrameContext)
  {
    Objects.requireNonNull(inFrameContext, "inFrameContext");

    this.frameInformation =
      Objects.requireNonNull(inFrameInformation, "frameInformation");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");

    this.portValues =
      new HashMap<>();
    this.frameServices =
      new HashMap<>();

    this.frameServices.put(RCVulkanFrameContextType.class, inFrameContext);
  }

  @Override
  public RCFrameInformation frameInformation()
  {
    return this.frameInformation;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends RCGFrameScopedServiceType> T frameScopedService(
    final Class<T> serviceClass)
  {
    return (T) this.frameServices.get(serviceClass);
  }

  @Override
  public <T> void portWrite(
    final RCGPortProducer<T> port,
    final T value)
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(value, "value");
    this.portValues.put(port, value);
  }

  @Override
  public <T> boolean portIsWritten(
    final RCGPortProducer<T> port)
  {
    Objects.requireNonNull(port, "port");
    return this.portValues.containsKey(port);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T portWritten(
    final RCGPortProducer<T> port)
  {
    Objects.requireNonNull(port, "port");
    return (T) this.portValues.get(port);
  }

  @Override
  public <T> T portRead(
    final RCGPortConsumer<T> port)
  {
    Objects.requireNonNull(port, "port");

    final var edges =
      this.graph.incomingEdgesOf(port);

    for (final var edge : edges) {
      return switch (edge.source()) {
        case final RCGPortModifier<?> v -> {
          yield (T) this.portRead(v);
        }
        case final RCGPortProducer<?> v -> {
          yield (T) this.portWritten(v);
        }
      };
    }

    throw new UnreachableCodeException();
  }

  @Override
  public <T> T portRead(
    final RCGPortModifier<T> port)
  {
    Objects.requireNonNull(port, "port");

    final var edges =
      this.graph.incomingEdgesOf(port);

    for (final var edge : edges) {
      return switch (edge.source()) {
        case final RCGPortModifier<?> v -> {
          yield (T) this.portRead(v);
        }
        case final RCGPortProducer<?> v -> {
          yield (T) this.portWritten(v);
        }
      };
    }

    throw new UnreachableCodeException();
  }
}
