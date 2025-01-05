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

import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGCommandType;
import com.io7m.rocaro.api.graph.RCGExecutionPlanType;
import com.io7m.rocaro.api.graph.RCGExecutionSubmissionType;
import com.io7m.rocaro.api.graph.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph.RCGGraphConnection;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGGraphOpConnection;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationFactoryType;
import com.io7m.rocaro.api.graph.RCGOperationFrameAcquireType;
import com.io7m.rocaro.api.graph.RCGOperationFramePresentType;
import com.io7m.rocaro.api.graph.RCGOperationImageLayoutTransitionType;
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
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourceSubname;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.api.render_targets.RCPresentationRenderTargetSchematicType;
import com.io7m.rocaro.api.resources.RCResourceSchematicPrimitiveType;
import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintType;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.graph.layout_transitions.RCGPassImageLayoutTransitions;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGGraphPrimitiveConnection;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPassPortPrimitive;
import com.io7m.rocaro.vanilla.internal.graph.port_primitive.RCGPortPrimitiveType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSExecute;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncCommandType;
import com.io7m.rocaro.vanilla.internal.graph.sync_primitive.RCGSyncDependency;
import com.io7m.rocaro.vanilla.internal.vulkan.RCFrameImageSchematic;
import com.io7m.rocaro.vanilla.internal.vulkan.RCFrameRenderTargetSchematic;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.rocaro.api.graph.RCNoParameters.NO_PARAMETERS;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_ALREADY_BUILT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_OPERATION_NAME_USED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_OPERATION_NOT_DECLARED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_ALREADY_CONNECTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_PORT_RESOURCE_ALREADY_ASSIGNED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_RESOURCE_ALREADY_ASSIGNED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_RESOURCE_NAME_USED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.OPERATION;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.PORT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.PORT_ASSIGNED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.PORT_REQUESTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.RESOURCE;

/**
 * The default mutable graph builder.
 */

public final class RCGGraphBuilder
  implements RCGGraphBuilderType,
  RCGGraphBuilderInternalType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGGraphBuilder.class);

  private final AtomicBoolean built;
  private final HashMap<RCGOperationName, RCGOperationType> ops;
  private final HashMap<RCGResourceName, RCGResourceVariable<?>> resources;
  private final DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> graph;
  private final DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph;
  private final HashMap<RCGPortProducerType<?>, RCGResourceVariable<?>> portResources;
  private final HashMap<RCGResourceVariable<?>, RCGPortProducerType<?>> resourcePorts;
  private final List<RCGGraphPassType> passes;
  private final HashMap<RCGPortType<?>, RCGResourceVariable<?>> portResourcesTracked;
  private final HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> portImageLayouts;
  private final HashMap<RCGOperationType, List<RCGCommandType>> opCommands;
  private final RCGraphName name;
  private final DirectedAcyclicGraph<RCGSyncCommandType, RCGSyncDependency> syncGraph;
  private final HashMap<RCGOperationType, RCGSExecute> syncOpCommands;
  private final RCStrings strings;
  private final HashSet<Class<? extends RCGGraphPassType>> passesExecuted;
  private VulkanPhysicalDeviceFeatures requiredFeatures;
  private List<RCGPortType<?>> portsOrdered;
  private List<RCGOperationType> opsOrdered;
  private SortedMap<RCGSubmissionID, RCGExecutionSubmissionType> submissions;
  private final DirectedAcyclicGraph<RCGPortPrimitiveType, RCGGraphPrimitiveConnection> primitivePortGraph;
  private final HashMap<RCGPortType<?>, HashMap<RCGResourceSubname, RCGPortPrimitiveType>> portToPrimitives;
  private final HashMap<RCGPortPrimitiveType, RCGResourceVariable<? extends RCResourceSchematicPrimitiveType>> portPrimitiveResourcesTracked;
  private List<RCGPortPrimitiveType> primitivePortsOrdered;

  private record RCGraphPassDependency(
    RCGGraphPassType before,
    RCGGraphPassType after)
  {
    RCGraphPassDependency
    {
      Objects.requireNonNull(before, "before");
      Objects.requireNonNull(after, "after");
    }
  }

  /**
   * The default mutable graph builder.
   *
   * @param inStrings The string resources
   * @param inName    The graph name
   */

  public RCGGraphBuilder(
    final RCStrings inStrings,
    final RCGraphName inName)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.name =
      Objects.requireNonNull(inName, "name");

    this.built =
      new AtomicBoolean(false);

    this.passes =
      List.copyOf(createGraphPasses());
    this.passesExecuted =
      new HashSet<>();

    this.graph =
      new DirectedAcyclicGraph<>(RCGGraphConnection.class);
    this.opGraph =
      new DirectedAcyclicGraph<>(RCGGraphOpConnection.class);
    this.syncGraph =
      new DirectedAcyclicGraph<>(RCGSyncDependency.class);
    this.primitivePortGraph =
      new DirectedAcyclicGraph<>(RCGGraphPrimitiveConnection.class);
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
    this.portToPrimitives =
      new HashMap<>();
    this.portPrimitiveResourcesTracked =
      new HashMap<>();
  }

  private static ArrayList<RCGGraphPassType> createGraphPasses()
  {
    final DirectedAcyclicGraph<RCGGraphPassType, RCGraphPassDependency> passGraph =
      new DirectedAcyclicGraph<>(RCGraphPassDependency.class);

    final var passSet =
      Stream.of(
        new RCGPassCheckNonEmpty(),
        new RCGPassCheckPortResourcesAssigned(),
        new RCGPassCheckPortsConnected(),
        new RCGPassCheckTypes(),
        new RCGPassDeviceFeatures(),
        new RCGPassImageLayoutTransitions(),
        new RCGPassPortPrimitive(),
        new RCGPassPrimitiveResourcesTrack(),
        new RCGPassPrimitiveTopological(),
        new RCGPassResourcesTrack(),
        new RCGPassTopological()
      ).collect(Collectors.toMap(RCGGraphPassType::getClass, p -> p));

    for (final var pass : passSet.values()) {
      passGraph.addVertex(pass);
    }

    for (final var beforePass : passSet.values()) {
      for (final var afterClass : beforePass.executesAfter()) {
        Preconditions.checkPreconditionV(
          passSet,
          passSet.containsKey(afterClass),
          "Pass set must contain %s",
          afterClass.getSimpleName()
        );

        final var afterPass =
          passSet.get(afterClass);

        passGraph.addEdge(
          beforePass,
          afterPass,
          new RCGraphPassDependency(beforePass, afterPass)
        );
      }
    }

    final var iter =
      new TopologicalOrderIterator<>(passGraph);
    final var passList =
      new ArrayList<RCGGraphPassType>();

    while (iter.hasNext()) {
      passList.addFirst(iter.next());
    }

    if (LOG.isTraceEnabled()) {
      for (int index = 0; index < passList.size(); ++index) {
        LOG.trace("Graph pass [{}]: {}", index, passList.get(index));
      }
    }

    for (final var pass : passSet.values()) {
      Postconditions.checkPostconditionV(
        passList,
        passList.contains(pass),
        "Pass list must contain %s",
        pass.getClass().getSimpleName()
      );
    }
    return passList;
  }

  private RCGGraphException errorOperationNameUsed(
    final RCGOperationName opName)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_OPERATION_NAME_USED),
      Map.of(this.strings.format(OPERATION), opName.value()),
      "error-graph-name-duplicate",
      Optional.empty()
    );
  }

  private RCGGraphException errorResourceNameUsed(
    final RCGResourceName resName)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_RESOURCE_NAME_USED),
      Map.of(this.strings.format(RESOURCE), resName.value()),
      "error-graph-name-duplicate",
      Optional.empty()
    );
  }

  private RCGGraphException errorResourceAlreadyAssigned(
    final RCGPortProducerType<?> requested,
    final RCGPortType<?> existing,
    final RCGResourceVariable<?> resource)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_RESOURCE_ALREADY_ASSIGNED),
      Map.ofEntries(
        Map.entry(
          this.strings.format(PORT_ASSIGNED),
          existing.name().value()
        ),
        Map.entry(
          this.strings.format(PORT_REQUESTED),
          requested.name().value()
        ),
        Map.entry(
          this.strings.format(RESOURCE),
          resource.name().value()
        )
      ),
      "error-graph-resource-already-assigned",
      Optional.empty()
    );
  }

  private RCGGraphException errorPortAlreadyHasResource(
    final RCGPortType<?> requested,
    final RCGResourceVariable<?> resource)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_PORT_RESOURCE_ALREADY_ASSIGNED),
      Map.ofEntries(
        Map.entry(
          this.strings.format(PORT_REQUESTED),
          requested.name().value()
        ),
        Map.entry(
          this.strings.format(RESOURCE),
          resource.name().value()
        )
      ),
      "error-graph-port-already-assigned",
      Optional.empty()
    );
  }

  private RCGGraphException errorPortAlreadyConnected(
    final RCGPortType<?> port)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_PORT_ALREADY_CONNECTED),
      Map.ofEntries(
        Map.entry(this.strings.format(PORT), port.name().value())
      ),
      "error-graph-port-already-connected",
      Optional.empty()
    );
  }

  private RCGGraphException errorOperationNonexistent(
    final RCGOperationType owner)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_OPERATION_NOT_DECLARED),
      Map.ofEntries(
        Map.entry(this.strings.format(OPERATION), owner.name().value())
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
  public HashMap<RCGPortType<?>, RCGResourceVariable<?>> portResourcesTracked()
  {
    return this.portResourcesTracked;
  }

  @Override
  public HashMap<RCGPortPrimitiveType, RCGResourceVariable<? extends RCResourceSchematicPrimitiveType>> portPrimitiveResourcesTracked()
  {
    return this.portPrimitiveResourcesTracked;
  }

  @Override
  public DirectedAcyclicGraph<RCGOperationType, RCGGraphOpConnection> opGraph()
  {
    return this.opGraph;
  }

  @Override
  public DirectedAcyclicGraph<RCGPortType<?>, RCGGraphConnection> graph()
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
  public HashMap<RCGPortProducerType<?>, RCGResourceVariable<?>> portResources()
  {
    return this.portResources;
  }

  @Override
  public HashMap<RCGResourceVariable<?>, RCGPortProducerType<?>> resourcePorts()
  {
    return this.resourcePorts;
  }

  @Override
  public HashMap<RCGResourceName, RCGResourceVariable<?>> resources()
  {
    return this.resources;
  }

  @Override
  public HashMap<RCGPortPrimitiveType, RCGOperationImageLayoutTransitionType> portImageLayouts()
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
    final List<RCGPortType<?>> ports)
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
  public List<RCGPortType<?>> portsOrdered()
  {
    if (this.portsOrdered == null) {
      throw new IllegalStateException("Ports not assigned.");
    }
    return this.portsOrdered;
  }

  @Override
  public void setSubmissions(
    final SortedMap<RCGSubmissionID, RCGExecutionSubmissionType> inSubmissions)
  {
    this.submissions = inSubmissions;
  }

  @Override
  public HashMap<RCGPortType<?>, HashMap<RCGResourceSubname, RCGPortPrimitiveType>> portToPrimitives()
  {
    return this.portToPrimitives;
  }

  @Override
  public DirectedAcyclicGraph<RCGPortPrimitiveType, RCGGraphPrimitiveConnection> primitivePortGraph()
  {
    return this.primitivePortGraph;
  }

  @Override
  public void setPrimitivePortsOrdered(
    final List<RCGPortPrimitiveType> ports)
  {
    this.primitivePortsOrdered = List.copyOf(ports);
  }

  @Override
  public List<RCGPortPrimitiveType> primitivePortsOrdered()
  {
    if (this.primitivePortsOrdered == null) {
      throw new IllegalStateException("Primitive ports not assigned.");
    }
    return this.primitivePortsOrdered;
  }

  private void checkNotBuilt()
    throws RCGGraphException
  {
    if (this.built.get()) {
      throw new RCGGraphException(
        this.strings.format(ERROR_GRAPH_ALREADY_BUILT),
        Map.of(),
        "error-graph-builder-used",
        Optional.empty()
      );
    }
  }


  @Override
  public RCGResourceVariable<
    RCPresentationRenderTargetSchematicType>
  declareFrameResource(
    final RCGResourceName resourceName)
    throws RCGGraphException
  {
    return this.declareResource(
      resourceName,
      new RCFrameRenderTargetSchematic(
        new RCFrameImageSchematic(
          Vector2I.of(0, 0),
          VulkanFormat.VK_FORMAT_B8G8R8A8_UNORM
        )
      )
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
      throw this.errorOperationNameUsed(operationName);
    }

    final var op = factory.create(this, operationName, parameters);
    this.ops.put(operationName, op);
    this.opGraph.addVertex(op);

    for (final var p : op.ports().values()) {
      this.graph.addVertex(p);
    }
    return op;
  }

  @Override
  public <S extends RCResourceSchematicType>
  RCGResourceVariable<S>
  declareResource(
    final RCGResourceName resourceName,
    final S parameters)
    throws RCGGraphException
  {
    Objects.requireNonNull(resourceName, "name");
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotBuilt();

    if (this.resources.containsKey(resourceName)) {
      throw this.errorResourceNameUsed(resourceName);
    }

    final var rv = new RCGResourceVariable<>(resourceName, parameters);
    this.resources.put(resourceName, rv);
    return rv;
  }

  @Override
  public <R extends RCResourceType, S extends RCResourceSchematicType> void
  resourceAssign(
    final RCGPortProducerType<? extends R> port,
    final RCGResourceVariable<? extends S> resource)
    throws RCGGraphException
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(resource, "resource");

    this.checkNotBuilt();

    {
      final var existing = this.resourceForPort(port);
      if (existing != null) {
        throw this.errorPortAlreadyHasResource(port, resource);
      }
    }

    {
      final var existing = this.portForResource(resource);
      if (existing != null) {
        throw this.errorResourceAlreadyAssigned(port, existing, resource);
      }
    }

    this.resourcePorts.put(resource, port);
    this.portResources.put(port, resource);
  }

  @SuppressWarnings("unchecked")
  private <R extends RCResourceType> RCGPortType<R> portForResource(
    final RCGResourceVariable<?> resource)
  {
    return (RCGPortType<R>) this.resourcePorts.get(resource);
  }

  @SuppressWarnings("unchecked")
  private <R extends RCResourceType> RCGResourceVariable<?> resourceForPort(
    final RCGPortProducerType<R> port)
  {
    return this.portResources.get(port);
  }

  @Override
  public <T extends RCResourceType, S extends T> void connect(
    final RCGPortSourceType<S> source,
    final RCGPortTargetType<T> target)
    throws RCGGraphException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    this.checkNotBuilt();

    final var sourceOp = source.owner();
    if (!this.operationIsDeclared(sourceOp)) {
      throw this.errorOperationNonexistent(sourceOp);
    }
    final var targetOp = target.owner();
    if (!this.operationIsDeclared(targetOp)) {
      throw this.errorOperationNonexistent(targetOp);
    }
    if (this.portIsConnected(source)) {
      throw this.errorPortAlreadyConnected(source);
    }
    if (this.portIsConnected(target)) {
      throw this.errorPortAlreadyConnected(target);
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
    final RCGPortType<?> port)
  {
    if (!this.graph.containsVertex(port)) {
      return false;
    }

    return switch (port) {
      case final RCGPortModifierType<?> _ -> {
        yield this.graph.degreeOf(port) == 2;
      }
      case final RCGPortConsumerType<?> _,
           final RCGPortProducerType<?> _ -> {
        yield this.graph.degreeOf(port) == 1;
      }

    };
  }

  @Override
  public RCGGraphType compile()
    throws RCGGraphException
  {
    this.checkNotBuilt();

    this.passesExecuted.clear();
    for (final var check : this.passes) {
      for (final var dependency : check.executesAfter()) {
        Preconditions.checkPreconditionV(
          this.passesExecuted,
          this.passesExecuted.contains(dependency),
          "Pass %s must have executed before pass %s".formatted(
            dependency.getSimpleName(),
            check.getClass().getSimpleName()
          )
        );
      }
      check.process(this);
      this.passesExecuted.add(check.getClass());
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
      Map.ofEntries(),
      this.requiredFeatures,
      this.makePlan()
    );
  }

  private RCGExecutionPlanType makePlan()
  {
    final var outSubmissions =
      new ArrayList<RCGExecutionSubmissionType>(this.submissions.size());

    for (final var entry : this.submissions.entrySet()) {
      outSubmissions.add(entry.getValue());
    }

    return new RCGPlan(outSubmissions);
  }

  private static final class RCGPlan
    implements RCGExecutionPlanType
  {
    private final List<RCGExecutionSubmissionType> submissions;

    private RCGPlan(
      final List<RCGExecutionSubmissionType> inSubmissions)
    {
      this.submissions = List.copyOf(inSubmissions);
    }

    @Override
    public List<RCGExecutionSubmissionType> submissions()
    {
      return this.submissions;
    }
  }

  @Override
  public RCGraphName name()
  {
    return this.name;
  }

  @Override
  public <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortProducerType<R> createProducerPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCSchematicConstraintType<S> typeConstraint,
    final Set<RCGCommandPipelineStage> writesAtStages)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConstraint, "typeConstraint");
    Objects.requireNonNull(writesAtStages, "writesAtStages");

    return new RCGPortProducer<>(
      owner,
      portName,
      readsAtStages,
      typeConstraint,
      writesAtStages
    );
  }

  @Override
  public <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortModifierType<R> createModifierPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCSchematicConstraintType<S> typeConsumes,
    final Set<RCGCommandPipelineStage> writesAtStages,
    final RCSchematicConstraintType<S> typeProduces)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConsumes, "typeConsumes");
    Objects.requireNonNull(writesAtStages, "writesAtStages");
    Objects.requireNonNull(typeProduces, "typeProduces");

    return new RCGPortModifier<>(
      owner,
      portName,
      readsAtStages,
      typeConsumes,
      writesAtStages,
      typeProduces
    );
  }

  @Override
  public <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortConsumerType<R> createConsumerPort(
    final RCGOperationType owner,
    final RCGPortName portName,
    final Set<RCGCommandPipelineStage> readsAtStages,
    final RCSchematicConstraintType<S> typeConstraint,
    final Set<RCGCommandPipelineStage> writesAtStages)
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(portName, "name");
    Objects.requireNonNull(readsAtStages, "readsAtStages");
    Objects.requireNonNull(typeConstraint, "typeConstraint");
    Objects.requireNonNull(writesAtStages, "writesAtStages");

    return new RCGPortConsumer<>(
      owner,
      portName,
      readsAtStages,
      typeConstraint,
      writesAtStages
    );
  }
}
