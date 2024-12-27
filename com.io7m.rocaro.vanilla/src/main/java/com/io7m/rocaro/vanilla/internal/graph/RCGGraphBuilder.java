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

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGCommandType;
import com.io7m.rocaro.api.graph.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGGraphOpConnection;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationFrameAcquireType;
import com.io7m.rocaro.api.graph.RCGOperationFramePresentType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType.Constant;
import com.io7m.rocaro.api.graph.RCGOperationName;
import com.io7m.rocaro.api.graph.RCGOperationParametersType;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortModifierType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortTargetType;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintType;
import com.io7m.rocaro.api.graph.RCGResourceFactoryType;
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourceParametersType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderFrameImageType;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGPassSync;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync.RCGSyncDependency;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.io7m.rocaro.api.graph.RCGNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_UNDEFINED;

/**
 * The default mutable graph builder.
 */

public final class RCGGraphBuilder
  implements RCGGraphBuilderType,
  RCGGraphBuilderInternalType
{
  private final AtomicBoolean built;
  private final HashMap<RCGOperationName, RCGOperationType> ops;
  private final HashMap<RCGResourceName, RCGResourcePlaceholderType> resources;
  private final DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph;
  private final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph;
  private final HashMap<RCGPortProducerType, RCGResourcePlaceholderType> portResources;
  private final HashMap<RCGResourcePlaceholderType, RCGPortProducerType> resourcePorts;
  private final List<RCGGraphPassType> passes;
  private final HashMap<RCGPortType, RCGResourcePlaceholderType> portResourcesTracked;
  private final HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> portImageLayouts;
  private final HashMap<RCGOperationType, List<RCGCommandType>> opCommands;
  private final RCGraphName name;
  private final DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph;
  private final HashMap<RCGOperationType, RCGSExecute> syncOpCommands;
  private VulkanPhysicalDeviceFeatures requiredFeatures;
  private List<RCGPortType> portsOrdered;
  private List<RCGOperationType> opsOrdered;

  /**
   * The default mutable graph builder.
   *
   * @param inName The graph name
   */

  public RCGGraphBuilder(
    final RCGraphName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");

    this.built =
      new AtomicBoolean(false);

    this.passes =
      List.of(
        new RCGPassCheckNonEmpty(),
        new RCGPassCheckPortsConnected(),
        new RCGPassCheckPortResourcesAssigned(),
        new RCGPassTopological(),
        new RCGPassDeviceFeatures(),
        new RCGPassResourcesTrack(),
        new RCGPassCheckTypes(),
        new RCGPassImageLayoutTransitions(),
        new RCGPassSync()
      );

    this.graph =
      new DirectedAcyclicGraph<>(RCGGraphConnection.class);
    this.opGraph =
      new DirectedAcyclicGraph<>(RCGGraphOpConnection.class);
    this.syncGraph =
      new DirectedAcyclicGraph<>(RCGSyncDependency.class);
    this.syncOpCommands =
      new HashMap<>();

    this.requiredFeatures =
      VulkanPhysicalDeviceFeaturesFunctions.none();
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
    this.opCommands =
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
    final RCGPortProducerType requested,
    final RCGPortType existing,
    final RCGResourcePlaceholderType resource)
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
    final RCGResourcePlaceholderType resource)
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

  @Override
  public HashMap<RCGOperationType, List<RCGCommandType>> opCommands()
  {
    return this.opCommands;
  }

  @Override
  public HashMap<RCGPortType, RCGResourcePlaceholderType> portResourcesTracked()
  {
    return this.portResourcesTracked;
  }

  @Override
  public DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph()
  {
    return this.opGraph;
  }

  @Override
  public DirectedAcyclicGraph<RCGPortType, RCGGraphConnection> graph()
  {
    return this.graph;
  }

  @Override
  public DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph()
  {
    return this.syncGraph;
  }

  @Override
  public HashMap<RCGOperationType, RCGSExecute> syncOpCommands()
  {
    return this.syncOpCommands;
  }

  @Override
  public HashMap<RCGOperationName, RCGOperationType> operations()
  {
    return this.ops;
  }

  @Override
  public HashMap<RCGPortProducerType, RCGResourcePlaceholderType> portResources()
  {
    return this.portResources;
  }

  @Override
  public HashMap<RCGResourcePlaceholderType, RCGPortProducerType> resourcePorts()
  {
    return this.resourcePorts;
  }

  @Override
  public HashMap<RCGResourceName, RCGResourcePlaceholderType> resources()
  {
    return this.resources;
  }

  @Override
  public HashMap<RCGPortType, RCGOperationImageLayoutTransitionType> portImageLayouts()
  {
    return this.portImageLayouts;
  }

  @Override
  public void traverse(
    final Consumer<RCGOperationType> receiver)
  {
    final var iter =
      new TopologicalOrderIterator<>(this.opGraph);

    while (iter.hasNext()) {
      receiver.accept(iter.next());
    }
  }

  @Override
  public void setRequiredFeatures(
    final VulkanPhysicalDeviceFeatures features)
  {
    this.requiredFeatures = Objects.requireNonNull(features, "features");
  }

  @Override
  public void setPortsOrdered(
    final List<RCGPortType> ports)
  {
    this.portsOrdered = List.copyOf(ports);
  }

  @Override
  public void setOpsOrdered(
    final List<RCGOperationType> operations)
  {
    this.opsOrdered = List.copyOf(operations);
  }

  @Override
  public List<RCGOperationType> opsOrdered()
  {
    if (this.opsOrdered == null) {
      throw new IllegalStateException("Ops not assigned.");
    }
    return this.opsOrdered;
  }

  @Override
  public List<RCGPortType> portsOrdered()
  {
    if (this.portsOrdered == null) {
      throw new IllegalStateException("Ports not assigned.");
    }
    return this.portsOrdered;
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
  public RCGResourcePlaceholderFrameImageType declareFrameResource(
    final RCGResourceName resourceName)
    throws RCGGraphException
  {
    return this.declareResource(
      resourceName,
      (n, _) -> new RCGResourcePlaceholderFrameImage(n),
      NO_PARAMETERS
    );
  }

  @Override
  public RCGOperationFrameAcquireType declareOpFrameAcquire(
    final RCGOperationName operationName)
    throws RCGGraphException
  {
    return this.declareOperation(
      operationName,
      (context, n, _) -> new RCGOperationFrameAcquire(context, n),
      NO_PARAMETERS
    );
  }

  @Override
  public RCGOperationFramePresentType declareOpFramePresent(
    final RCGOperationName operationName)
    throws RCGGraphException
  {
    return this.declareOperation(
      operationName,
      (context, n, _) -> new RCGOperationFramePresent(context, n),
      NO_PARAMETERS
    );
  }

  @Override
  public <P extends RCGOperationParametersType, O extends RCGOperationType> O
  declareOperation(
    final RCGOperationName operationName,
    final RCGOperationFactoryType<P, O> factory,
    final P parameters)
    throws RCGGraphException
  {
    Objects.requireNonNull(operationName, "name");
    Objects.requireNonNull(factory, "factory");
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotBuilt();

    if (this.ops.containsKey(operationName)) {
      throw errorOperationNameUsed(operationName);
    }

    final var op = factory.create(this, operationName, parameters);
    this.ops.put(operationName, op);
    this.opGraph.addVertex(op);

    for (final var p : op.ports().values()) {
      this.graph.addVertex(p);
      this.portImageLayouts.put(p, new Constant(LAYOUT_UNDEFINED));
    }
    return op;
  }

  @Override
  public <P extends RCGResourceParametersType, R extends RCGResourcePlaceholderType> R
  declareResource(
    final RCGResourceName operationName,
    final RCGResourceFactoryType<P, R> factory,
    final P parameters)
    throws RCGGraphException
  {
    Objects.requireNonNull(operationName, "name");
    Objects.requireNonNull(factory, "factory");
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotBuilt();

    if (this.resources.containsKey(operationName)) {
      throw errorResourceNameUsed(operationName);
    }

    final var r = factory.create(operationName, parameters);
    this.resources.put(operationName, r);
    return r;
  }

  @Override
  public void resourceAssign(
    final RCGPortProducerType port,
    final RCGResourcePlaceholderType resource)
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
    final RCGResourcePlaceholderType resource)
  {
    return this.resourcePorts.get(resource);
  }

  private RCGResourcePlaceholderType resourceForPort(
    final RCGPortProducerType port)
  {
    return this.portResources.get(port);
  }

  @Override
  public void connect(
    final RCGPortSourceType source,
    final RCGPortTargetType target)
    throws RCGGraphException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    this.checkNotBuilt();

    final var sourceOp = source.owner();
    if (!this.operationIsDeclared(sourceOp)) {
      throw errorOperationNonexistent(sourceOp);
    }
    final var targetOp = target.owner();
    if (!this.operationIsDeclared(targetOp)) {
      throw errorOperationNonexistent(targetOp);
    }
    if (this.portIsConnected(source)) {
      throw errorPortAlreadyConnected(source);
    }
    if (this.portIsConnected(target)) {
      throw errorPortAlreadyConnected(target);
    }

    {
      final var connection = new RCGGraphOpConnection(sourceOp, targetOp);
      this.opGraph.addVertex(sourceOp);
      this.opGraph.addVertex(targetOp);
      if (!this.opGraph.containsEdge(connection)) {
        this.opGraph.addEdge(sourceOp, targetOp, connection);
      }
    }

    {
      final var connection = new RCGGraphConnection(source, target);
      this.graph.addVertex(source);
      this.graph.addVertex(target);
      this.graph.addEdge(source, target, connection);
    }
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
      case final RCGPortModifierType _ -> {
        yield this.graph.degreeOf(port) == 2;
      }
      case final RCGPortConsumerType _,
           final RCGPortProducerType _ -> {
        yield this.graph.degreeOf(port) == 1;
      }

    };
  }

  @Override
  public RCGGraphType compile()
    throws RCGGraphException
  {
    this.checkNotBuilt();

    for (final var check : this.passes) {
      check.process(this);
    }

    this.built.set(true);
    return new RCGGraph(
      this.name,
      this.graph,
      this.opGraph,
      this.portsOrdered,
      this.opsOrdered,
      Map.copyOf(this.ops),
      Map.copyOf(this.resources),
      Map.copyOf(this.portResourcesTracked),
      Map.copyOf(this.portImageLayouts),
      this.requiredFeatures
    );
  }

  @Override
  public RCGraphName name()
  {
    return this.name;
  }

  @Override
  public <R extends RCGResourcePlaceholderType> RCGPortProducerType
  createProducerPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCGPortTypeConstraintType<R> typeConstraint,
    final Set<RCGCommandPipelineStage> writesAtStages)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConstraint, "typeConstraint");
    Objects.requireNonNull(writesAtStages, "writesAtStages");

    return new RCGPortProducer(
      owner,
      portName,
      readsAtStages,
      typeConstraint,
      writesAtStages
    );
  }

  @Override
  public <R extends RCGResourcePlaceholderType> RCGPortModifierType
  createModifierPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCGPortTypeConstraintType<R> typeConsumes,
    final Set<RCGCommandPipelineStage> writesAtStages,
    final RCGPortTypeConstraintType<R> typeProduces)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConsumes, "typeConsumes");
    Objects.requireNonNull(writesAtStages, "writesAtStages");
    Objects.requireNonNull(typeProduces, "typeProduces");

    return new RCGPortModifier(
      owner,
      portName,
      readsAtStages,
      typeConsumes,
      writesAtStages,
      typeProduces
    );
  }

  @Override
  public <R extends RCGResourcePlaceholderType> RCGPortConsumerType
  createConsumerPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCGPortTypeConstraintType<R> typeConstraint,
    final Set<RCGCommandPipelineStage> writesAtStages)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConstraint, "typeConstraint");
    Objects.requireNonNull(writesAtStages, "writesAtStages");

    return new RCGPortConsumer(
      owner,
      portName,
      readsAtStages,
      typeConstraint,
      writesAtStages
    );
  }
}
