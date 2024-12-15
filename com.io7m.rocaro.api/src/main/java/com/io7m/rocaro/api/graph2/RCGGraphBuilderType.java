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
 * The type of mutable builders to construct graphs.
 */

public interface RCGGraphBuilderType
{
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
   * @param factory    The resource factory
   * @param parameters The resource parameters
   * @param <P>        The type of parameters
   * @param <R>        The type of resource
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  <P extends RCGResourceParametersType, R extends RCGResourceType>
  R declareResource(
    RCGResourceName name,
    RCGResourceFactoryType<P, R> factory,
    P parameters)
    throws RCGGraphException;

  /**
   * Declare a resource.
   *
   * @param name       The resource name
   * @param factory    The resource factory
   * @param parameters The resource parameters
   * @param <P>        The type of parameters
   * @param <R>        The type of resource
   *
   * @return The resource
   *
   * @throws RCGGraphException On errors
   */

  default <P extends RCGResourceParametersType, R extends RCGResourceType>
  R declareResource(
    final String name,
    final RCGResourceFactoryType<P, R> factory,
    final P parameters)
    throws RCGGraphException
  {
    return this.declareResource(
      new RCGResourceName(name),
      factory,
      parameters
    );
  }

  /**
   * Assign a resource to a producer port.
   *
   * @param port     The port
   * @param resource The resource
   *
   * @throws RCGGraphException On errors
   */

  void resourceAssign(
    RCGPortProduces port,
    RCGResourceType resource)
    throws RCGGraphException;

  /**
   * Connect a producer port to a consumer port.
   *
   * @param source The source port
   * @param target The target port
   *
   * @throws RCGGraphException On errors
   */

  void connect(
    RCGPortProducerType source,
    RCGPortConsumerType target)
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
}
