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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.rocaro.api.graph2.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph2.RCGGraphException;
import com.io7m.rocaro.api.graph2.RCGGraphType;
import com.io7m.rocaro.api.graph2.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph2.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationParametersType;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortConsumerType;
import com.io7m.rocaro.api.graph2.RCGPortConsumes;
import com.io7m.rocaro.api.graph2.RCGPortModifies;
import com.io7m.rocaro.api.graph2.RCGPortProducerType;
import com.io7m.rocaro.api.graph2.RCGPortProduces;
import com.io7m.rocaro.api.graph2.RCGPortType;
import com.io7m.rocaro.api.graph2.RCGResourceFactoryType;
import com.io7m.rocaro.api.graph2.RCGResourceName;
import com.io7m.rocaro.api.graph2.RCGResourceParametersType;
import com.io7m.rocaro.api.graph2.RCGResourceType;
import com.io7m.rocaro.api.graph2.RCGResourceTypes;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_UNDEFINED;

/**
 * The default mutable graph builder.
 */

public final class RCGGraphBuilder implements RCGGraphBuilderType
{
  private final AtomicBoolean built;
  private final HashMap<RCGOperationName, RCGOperationType> ops;
  private final HashMap<RCGResourceName, RCGResourceType> resources;
  private final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph;
  private final HashMap<RCGPortProduces, RCGResourceType> portResources;
  private final HashMap<RCGResourceType, RCGPortProduces> resourcePorts;
  private final List<RCGGraphPassType> passes;
  private final HashMap<RCGPortType, RCGResourceType> portResourcesTracked;
  private final HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> portImageLayouts;

  /**
   * The default mutable graph builder.
   */

  public RCGGraphBuilder()
  {
    this.built =
      new AtomicBoolean(false);

    this.passes =
      List.of(
        new RCGPassCheckNonEmpty(),
        new RCGPassCheckPortsConnected(),
        new RCGPassCheckPortResourcesAssigned(),
        new RCGPassResourcesTrack(),
        new RCGPassImageLayoutTransitions()
      );

    this.graph =
      new DirectedAcyclicGraph<>(RCGGraphConnection.class);
    this.ops =
      new HashMap<>();
    this.resources =
      new HashMap<>();
    this.portResources =
      new HashMap<>();
    this.resourcePorts =
      new HashMap<>();
    this.portResourcesTracked =
      new HashMap<>();
    this.portImageLayouts =
      new HashMap<>();
  }

  private static RCGGraphException errorOperationNameUsed(
    final RCGOperationName name)
  {
    return new RCGGraphException(
      "Operation name already used.",
      Map.of("Operation", name.value()),
      "error-graph-name-duplicate",
      Optional.empty()
    );
  }

  private static RCGGraphException errorResourceNameUsed(
    final RCGResourceName name)
  {
    return new RCGGraphException(
      "Resource name already used.",
      Map.of("Resource", name.value()),
      "error-graph-name-duplicate",
      Optional.empty()
    );
  }

  private static RCGGraphException errorResourceAlreadyAssigned(
    final RCGPortProduces requested,
    final RCGPortType existing,
    final RCGResourceType resource)
  {
    return new RCGGraphException(
      "The given resource is already assigned to a different port.",
      Map.ofEntries(
        Map.entry("Port (Assigned)", existing.name().value()),
        Map.entry("Port (Requested)", requested.name().value()),
        Map.entry("Resource", resource.name().value())
      ),
      "error-graph-resource-already-assigned",
      Optional.empty()
    );
  }

  private static RCGGraphException errorPortAlreadyHasResource(
    final RCGPortType requested,
    final RCGResourceType resource)
  {
    return new RCGGraphException(
      "The given port already has an assigned resource.",
      Map.ofEntries(
        Map.entry("Port (Requested)", requested.name().value()),
        Map.entry("Resource", resource.name().value())
      ),
      "error-graph-port-already-assigned",
      Optional.empty()
    );
  }

  private static RCGGraphException errorSourceTargetPortsIncompatible(
    final RCGPortProducerType source,
    final RCGPortConsumerType target)
  {
    return new RCGGraphException(
      "Source and target ports are type-incompatible.",
      Map.ofEntries(
        Map.entry("Port (Source)", source.name().value()),
        Map.entry("Port (Target)", target.name().value()),
        Map.entry("Type (Source)", source.type().getName()),
        Map.entry("Type (Target)", target.type().getName())
      ),
      "error-graph-port-type-incompatible",
      Optional.empty()
    );
  }

  private static RCGGraphException errorPortAlreadyConnected(
    final RCGPortType port)
  {
    return new RCGGraphException(
      "Port is already connected.",
      Map.ofEntries(
        Map.entry("Port", port.name().value())
      ),
      "error-graph-port-already-connected",
      Optional.empty()
    );
  }

  private static RCGGraphException errorOperationNonexistent(
    final RCGOperationType owner)
  {
    return new RCGGraphException(
      "Operation not declared.",
      Map.ofEntries(
        Map.entry("Operation", owner.name().value())
      ),
      "error-graph-operation-not-declared",
      Optional.empty()
    );
  }

  /**
   * @return The resource at each port
   */

  public HashMap<RCGPortType, RCGResourceType> portResourcesTracked()
  {
    return this.portResourcesTracked;
  }

  /**
   * @return The graph
   */

  public DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph()
  {
    return this.graph;
  }

  /**
   * @return The operations
   */

  public HashMap<RCGOperationName, RCGOperationType> operations()
  {
    return this.ops;
  }

  /**
   * @return The resource for each port
   */

  public HashMap<RCGPortProduces, RCGResourceType> portResources()
  {
    return this.portResources;
  }

  /**
   * @return The port for each resource
   */

  public HashMap<RCGResourceType, RCGPortProduces> resourcePorts()
  {
    return this.resourcePorts;
  }

  /**
   * @return The resources
   */

  public HashMap<RCGResourceName, RCGResourceType> resources()
  {
    return this.resources;
  }

  /**
   * @return The image layouts per port
   */

  public HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> portImageLayouts()
  {
    return this.portImageLayouts;
  }

  private void checkNotBuilt()
    throws RCGGraphException
  {
    if (this.built.get()) {
      throw new RCGGraphException(
        "This graph builder has already been compiled.",
        Map.of(),
        "error-graph-builder-used",
        Optional.empty()
      );
    }
  }

  @Override
  public <P extends RCGOperationParametersType, O extends RCGOperationType> O
  declareOperation(
    final RCGOperationName name,
    final RCGOperationFactoryType<P, O> factory,
    final P parameters)
    throws RCGGraphException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(factory, "factory");
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotBuilt();

    if (this.ops.containsKey(name)) {
      throw errorOperationNameUsed(name);
    }

    final var op = factory.create(name, parameters);
    this.ops.put(name, op);

    for (final var p : op.ports()) {
      this.graph.addVertex(p);
      this.portImageLayouts.put(p, new Constant(LAYOUT_UNDEFINED));
    }
    return op;
  }

  @Override
  public <P extends RCGResourceParametersType, R extends RCGResourceType> R
  declareResource(
    final RCGResourceName name,
    final RCGResourceFactoryType<P, R> factory,
    final P parameters)
    throws RCGGraphException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(factory, "factory");
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotBuilt();

    if (this.resources.containsKey(name)) {
      throw errorResourceNameUsed(name);
    }

    final var r = factory.create(name, parameters);
    this.resources.put(name, r);
    return r;
  }

  @Override
  public void resourceAssign(
    final RCGPortProduces port,
    final RCGResourceType resource)
    throws RCGGraphException
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(resource, "resource");

    this.checkNotBuilt();

    {
      final var existing = this.resourceForPort(port);
      if (existing != null) {
        throw errorPortAlreadyHasResource(port, resource);
      }
    }

    {
      final var existing = this.portForResource(resource);
      if (existing != null) {
        throw errorResourceAlreadyAssigned(port, existing, resource);
      }
    }

    this.resourcePorts.put(resource, port);
    this.portResources.put(port, resource);
  }

  private RCGPortType portForResource(
    final RCGResourceType resource)
  {
    return this.resourcePorts.get(resource);
  }

  private RCGResourceType resourceForPort(
    final RCGPortProduces port)
  {
    return this.portResources.get(port);
  }

  @Override
  public void connect(
    final RCGPortProducerType source,
    final RCGPortConsumerType target)
    throws RCGGraphException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    this.checkNotBuilt();

    if (!RCGResourceTypes.targetCanAcceptSource(source, target)) {
      throw errorSourceTargetPortsIncompatible(source, target);
    }
    if (!this.operationIsDeclared(source.owner())) {
      throw errorOperationNonexistent(source.owner());
    }
    if (!this.operationIsDeclared(target.owner())) {
      throw errorOperationNonexistent(target.owner());
    }
    if (this.portIsConnected(source)) {
      throw errorPortAlreadyConnected(source);
    }
    if (this.portIsConnected(target)) {
      throw errorPortAlreadyConnected(target);
    }

    final var connection = new RCGGraphConnection(source, target);
    this.graph.addVertex(source);
    this.graph.addVertex(target);
    this.graph.addEdge(source, target, connection);
  }

  private boolean operationIsDeclared(
    final RCGOperationType op)
  {
    return Objects.equals(this.ops.get(op.name()), op);
  }

  private boolean portIsConnected(
    final RCGPortType port)
  {
    if (!this.graph.containsVertex(port)) {
      return false;
    }

    return switch (port) {
      case final RCGPortConsumes _,
           final RCGPortProduces _ -> {
        yield this.graph.degreeOf(port) == 1;
      }
      case final RCGPortModifies _ -> {
        yield this.graph.degreeOf(port) == 2;
      }
    };
  }

  @Override
  public RCGGraphType compile()
    throws RCGGraphException
  {
    this.checkNotBuilt();

    for (final var check : this.passes) {
      check.check(this);
    }

    this.built.set(true);
    return new RCGGraph(
      this.graph,
      Map.copyOf(this.ops),
      Map.copyOf(this.resources),
      Map.copyOf(this.portResourcesTracked),
      Map.copyOf(this.portImageLayouts)
    );
  }
}
