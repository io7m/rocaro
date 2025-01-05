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

import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetSchematicType;
import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;

/**
 * The type of mutable builders to construct graphs.
 */

public interface RCGGraphBuilderType
{
  /**
   * Declare a frame image resource.
   *
   * @param name The name
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  RCGResourceVariable<
    RCPresentationRenderTargetSchematicType>
  declareFrameResource(
    RCGResourceName name)
    throws RCGGraphException;

  /**
   * Declare a frame image resource.
   *
   * @param name The name
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  default RCGResourceVariable<
    RCPresentationRenderTargetSchematicType>
  declareFrameResource(
    final String name)
    throws RCGGraphException
  {
    return this.declareFrameResource(new RCGResourceName(name));
  }

  /**
   * Declare an operation that acquires a frame from the swapchain.
   *
   * @param name The operation name
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  RCGOperationFrameAcquireType declareOpFrameAcquire(
    RCGOperationName name)
    throws RCGGraphException;

  /**
   * Declare an operation that acquires a frame from the swapchain.
   *
   * @param name The operation name
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  default RCGOperationFrameAcquireType declareOpFrameAcquire(
    final String name)
    throws RCGGraphException
  {
    return this.declareOpFrameAcquire(new RCGOperationName(name));
  }

  /**
   * Declare an operation that presents a frame to the swapchain.
   *
   * @param name The operation name
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  RCGOperationFramePresentType declareOpFramePresent(
    RCGOperationName name)
    throws RCGGraphException;

  /**
   * Declare an operation that presents a frame to the swapchain.
   *
   * @param name The operation name
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  default RCGOperationFramePresentType declareOpFramePresent(
    final String name)
    throws RCGGraphException
  {
    return this.declareOpFramePresent(new RCGOperationName(name));
  }

  /**
   * Declare an operation.
   *
   * @param name       The operation name
   * @param factory    The operation factory
   * @param parameters The operation parameters
   * @param <P>        The type of parameters
   * @param <O>        The type of operation
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  <P extends RCGOperationParametersType, O extends RCGOperationType>
  O declareOperation(
    RCGOperationName name,
    RCGOperationFactoryType<P, O> factory,
    P parameters)
    throws RCGGraphException;

  /**
   * Declare an operation.
   *
   * @param name       The operation name
   * @param factory    The operation factory
   * @param parameters The operation parameters
   * @param <P>        The type of parameters
   * @param <O>        The type of operation
   *
   * @return The operation
   *
   * @throws RCGGraphException On errors
   */

  default <P extends RCGOperationParametersType, O extends RCGOperationType>
  O declareOperation(
    final String name,
    final RCGOperationFactoryType<P, O> factory,
    final P parameters)
    throws RCGGraphException
  {
    return this.declareOperation(
      new RCGOperationName(name),
      factory,
      parameters
    );
  }

  /**
   * Declare a resource.
   *
   * @param name       The resource name
   * @param parameters The resource parameters
   * @param <S>        The type of resource schematic
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  <S extends RCResourceSchematicType>
  RCGResourceVariable<S>
  declareResource(
    RCGResourceName name,
    S parameters)
    throws RCGGraphException;

  /**
   * Declare a resource.
   *
   * @param name       The resource name
   * @param parameters The resource parameters
   * @param <S>        The type of resource schematic
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  default <S extends RCResourceSchematicType>
  RCGResourceVariable<S>
  declareResource(
    final String name,
    final S parameters)
    throws RCGGraphException
  {
    return this.declareResource(
      new RCGResourceName(name),
      parameters
    );
  }

  /**
   * Assign a resource to a producer port.
   *
   * @param port     The port
   * @param resource The resource
   * @param <R>      The type of resource
   * @param <S>      The type of resource schematic
   *
   * @throws RCGGraphException On errors
   */

  <R extends RCResourceType, S extends RCResourceSchematicType>
  void resourceAssign(
    RCGPortProducerType<? extends R> port,
    RCGResourceVariable<? extends S> resource)
    throws RCGGraphException;

  /**
   * Connect a port to a port.
   *
   * @param source The source port
   * @param target The target port
   * @param <S>    The source resource type
   * @param <T>    The target resource type
   *
   * @throws RCGGraphException On errors
   */

  <T extends RCResourceType, S extends T>
  void connect(
    RCGPortSourceType<S> source,
    RCGPortTargetType<T> target)
    throws RCGGraphException;

  /**
   * Check and compile the graph.
   *
   * @return The graph
   *
   * @throws RCGGraphException On errors
   */

  RCGGraphType compile()
    throws RCGGraphException;

  /**
   * @return The graph name
   */

  RCGraphName name();
}
