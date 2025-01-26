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


package com.io7m.rocaro.rgraphc.internal.primitive_graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.access_set.RCTAccessSetSingletonType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCImageLayoutStatusType.Changed;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCImageLayoutStatusType.Unchanged;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeBranchedType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeLeafType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeSingletonType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTColorAttachment;
import com.io7m.rocaro.rgraphc.internal.typed.RCTDepthAttachment;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortConnection;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortConsumer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortModifier;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortProducer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPrimitiveResourceType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationBuffer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationImage;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarations;
import com.io7m.seltzer.api.SStructuredError;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.io7m.rocaro.api.graph.RCGResourceImageLayout.LAYOUT_UNDEFINED;
import static com.io7m.rocaro.rgraphc.internal.RCCPLexical.showPosition;

@JsonPropertyOrder(alphabetic = true)
public final class RCCPrimitivePortGraph
{
  private final Builder builder;

  private RCCPrimitivePortGraph(
    final Builder builder)
  {
    this.builder = Objects.requireNonNull(builder, "builder");
  }

  public static RCCPrimitivePortGraph create(
    final RCTGraphDeclarationType graph)
    throws RCCompilerException
  {
    return new Builder(graph).build();
  }

  public Graph<RCCPortPrimitiveType, RCCPortPrimitiveConnection> graph()
  {
    return this.builder.primitiveGraph;
  }

  public Set<RCCPortPrimitiveType> primitivePortsForOp(
    final RCCName name)
  {
    Objects.requireNonNull(name, "name");

    Preconditions.checkPreconditionV(
      this.builder.opPorts.containsKey(name),
      "Operation %s must exist",
      name
    );
    return this.builder.opPorts.get(name);
  }

  @JsonProperty("PortResources")
  @JsonPropertyOrder(alphabetic = true)
  public Map<RCCPortPath, RCCPPlaceholderType> portResources()
  {
    return Collections.unmodifiableMap(this.builder.portResources);
  }

  @JsonProperty("Resources")
  @JsonPropertyOrder(alphabetic = true)
  public Map<Long, RCCPPlaceholderType> resources()
  {
    return Collections.unmodifiableMap(this.builder.resources);
  }

  @JsonProperty("Connections")
  public Set<RCCPortPrimitiveConnection> connections()
  {
    return Collections.unmodifiableSet(
      new TreeSet<>(this.builder.primitiveGraph.edgeSet())
    );
  }

  @JsonProperty("ImageLayoutTransitions")
  @JsonPropertyOrder(alphabetic = true)
  public Map<RCCPortPath, RCCPortImageLayout> portImageLayoutTransitions()
  {
    return Collections.unmodifiableSortedMap(this.builder.portImageLayoutTransitions);
  }

  public RCCPPlaceholderType resourceForPort(
    final RCCPortPrimitiveType port)
  {
    Objects.requireNonNull(port, "port");

    Preconditions.checkPreconditionV(
      this.builder.portResources.containsKey(port.fullPath()),
      "Port %s must exist",
      port.fullPath()
    );

    return this.builder.portResources.get(port.fullPath());
  }

  public RCCPortImageLayout imageLayoutTransitionForPort(
    final RCCPortPrimitiveType port)
  {
    Objects.requireNonNull(port, "port");

    final var path =
      port.fullPath();
    final var r =
      this.builder.portImageLayoutTransitions.get(path);

    if (r == null) {
      throw new IllegalArgumentException(
        "No image layout transition for %s".formatted(path)
      );
    }
    return r;
  }

  private static final class Builder
  {
    private final RCTGraphDeclarationType graph;
    private final DirectedAcyclicGraph<RCCPortPrimitiveType, RCCPortPrimitiveConnection> primitiveGraph;
    private final HashMap<RCTPortType, HashMap<RCCPortPath, RCCPortPrimitiveType>> portsToPrimitives;
    private final TreeMap<Long, RCCPPlaceholderType> resources;
    private final HashMap<RCCPortPath, RCCPPlaceholderType> portResources;
    private final HashMap<RCCName, HashSet<RCCPortPrimitiveType>> opPorts;
    private final AtomicLong resourceIdPool;
    private final TreeMap<RCCPortPath, RCCPortImageLayout> portImageLayoutTransitions;

    Builder(
      final RCTGraphDeclarationType inGraph)
    {
      this.graph =
        Objects.requireNonNull(inGraph, "graph");
      this.primitiveGraph =
        new DirectedAcyclicGraph<>(RCCPortPrimitiveConnection.class);
      this.portsToPrimitives =
        new HashMap<>();
      this.resources =
        new TreeMap<>();
      this.resourceIdPool =
        new AtomicLong(0);
      this.portResources =
        new HashMap<>();
      this.opPorts =
        new HashMap<>();
      this.portImageLayoutTransitions =
        new TreeMap<>();
    }

    private static RCCPortPath fullPathOf(
      final RCTOperationDeclaration p,
      final RCCPath path)
    {
      return new RCCPortPath(p.name(), path);
    }

    private RCCPPlaceholderType freshPlaceholderFor(
      final RCTPrimitiveResourceType resource)
    {
      final var id =
        this.resourceIdPool.getAndIncrement();

      final RCCPPlaceholderType p =
        switch (resource) {
          case final RCTDepthAttachment _ -> {
            yield new RCCPPlaceholderAttachmentDepth(id);
          }
          case final RCTColorAttachment _ -> {
            yield new RCCPPlaceholderAttachmentColor(id);
          }
          case final RCTTypeDeclarationBuffer _ -> {
            yield new RCCPPlaceholderBuffer(id);
          }
          case final RCTTypeDeclarationImage _ -> {
            yield new RCCPPlaceholderImage(id);
          }
        };

      this.resources.put(id, p);
      return p;
    }

    RCCPrimitivePortGraph build()
      throws RCCompilerException
    {
      final var portGraph =
        this.graph.portGraph();

      this.processOps();
      this.processPortCreations(portGraph);
      this.processPortConnections(portGraph);
      this.processBindResources();
      this.processImageLayoutTransitions();

      return new RCCPrimitivePortGraph(this);
    }

    private void processOps()
    {
      for (final var op : this.graph.opsOrdered()) {
        this.processOp(op);
      }
    }

    private void processPortCreations(
      final Graph<RCTPortType, RCTPortConnection> portGraph)
      throws RCCompilerException
    {
      for (final var port : portGraph.vertexSet()) {
        this.processCreatePort(port);
      }
    }

    private void processPortConnections(
      final Graph<RCTPortType, RCTPortConnection> portGraph)
    {
      for (final var edge : portGraph.edgeSet()) {
        this.processPortConnection(edge);
      }
    }

    private void processImageLayoutTransitions()
    {
      final var producers =
        this.primitiveGraph.vertexSet()
          .stream()
          .filter(p -> p instanceof RCCPortPrimitiveProducer)
          .map(RCCPortPrimitiveProducer.class::cast)
          .collect(Collectors.toSet());

      for (final var p : producers) {
        this.tracePortImageTransitions(LAYOUT_UNDEFINED, p);
      }
    }

    private void tracePortImageTransitions(
      final RCGResourceImageLayout layoutThen,
      final RCCPortPrimitiveType port)
    {
      Preconditions.checkPreconditionV(
        !this.portImageLayoutTransitions.containsKey(port.fullPath()),
        "Port %s must not have a layout transition",
        port.fullPath()
      );

      if (!port.type().isImageType()) {
        return;
      }

      switch (port) {
        case final RCCPortPrimitiveModifier p -> {
          this.tracePortTransitionsModifier(layoutThen, p);
        }
        case final RCCPortPrimitiveProducer p -> {
          this.tracePortTransitionsProducer(p);
        }
        case final RCCPortPrimitiveConsumer p -> {
          this.tracePortTransitionsConsumer(layoutThen, p);
        }
      }
    }

    private void tracePortTransitionsModifier(
      final RCGResourceImageLayout layoutThen,
      final RCCPortPrimitiveModifier port)
    {
      Preconditions.checkPreconditionV(
        !this.portImageLayoutTransitions.containsKey(port.fullPath()),
        "Port %s must not have a layout transition",
        port.fullPath()
      );

      if (!port.type().isImageType()) {
        return;
      }

      final var requiresOpt =
        port.requiresImageLayout();
      final var ensuresOpt =
        port.ensuresImageLayout();

      this.tracePortTransitionsModifierActual(
        layoutThen,
        port,
        requiresOpt,
        ensuresOpt
      );
    }

    private void tracePortTransitionsModifierActual(
      final RCGResourceImageLayout layoutThen,
      final RCCPortPrimitiveModifier port,
      final Optional<RCGResourceImageLayout> requiresOpt,
      final Optional<RCGResourceImageLayout> ensuresOpt)
    {
      Preconditions.checkPreconditionV(
        !this.portImageLayoutTransitions.containsKey(port.fullPath()),
        "Port %s must not have a layout transition",
        port.fullPath()
      );

      if (!port.type().isImageType()) {
        return;
      }

      var layoutDuring = layoutThen;
      if (requiresOpt.isPresent()) {
        layoutDuring = requiresOpt.get();
      }

      var layoutLeaving = layoutDuring;
      if (ensuresOpt.isPresent()) {
        layoutLeaving = ensuresOpt.get();
      }

      if (layoutThen != layoutDuring) {
        if (layoutDuring != layoutLeaving) {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            new RCCPortImageLayout(
              new Changed(layoutThen, layoutDuring),
              new Changed(layoutDuring, layoutLeaving)
            )
          );
        } else {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            new RCCPortImageLayout(
              new Changed(layoutThen, layoutDuring),
              new Unchanged(layoutDuring)
            )
          );
        }
      } else {
        if (layoutDuring != layoutLeaving) {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            new RCCPortImageLayout(
              new Unchanged(layoutDuring),
              new Changed(layoutDuring, layoutLeaving)
            )
          );
        } else {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            new RCCPortImageLayout(
              new Unchanged(layoutThen),
              new Unchanged(layoutThen)
            )
          );
        }
      }

      for (final var connection : this.primitiveGraph.outgoingEdgesOf(port)) {
        this.tracePortImageTransitions(
          layoutLeaving,
          connection.targetPort()
        );
      }
    }

    private void tracePortTransitionsProducer(
      final RCCPortPrimitiveProducer port)
    {
      Preconditions.checkPreconditionV(
        !this.portImageLayoutTransitions.containsKey(port.fullPath()),
        "Port %s must not have a layout transition",
        port.fullPath()
      );

      if (!port.type().isImageType()) {
        return;
      }

      final var layoutNow =
        port.ensuresImageLayout()
          .orElseThrow();

      this.portImageLayoutTransitions.put(
        port.fullPath(),
        new RCCPortImageLayout(
          new Unchanged(layoutNow),
          new Unchanged(layoutNow)
        )
      );

      for (final var connection : this.primitiveGraph.outgoingEdgesOf(port)) {
        this.tracePortImageTransitions(
          layoutNow,
          connection.targetPort()
        );
      }
    }

    private void tracePortTransitionsConsumer(
      final RCGResourceImageLayout layoutThen,
      final RCCPortPrimitiveConsumer port)
    {
      Preconditions.checkPreconditionV(
        !this.portImageLayoutTransitions.containsKey(port.fullPath()),
        "Port %s must not have a layout transition",
        port.fullPath()
      );

      if (!port.type().isImageType()) {
        return;
      }

      final var requiresOpt =
        port.requiresImageLayout();

      final var unchanged =
        new RCCPortImageLayout(
          new Unchanged(layoutThen),
          new Unchanged(layoutThen)
        );

      if (requiresOpt.isPresent()) {
        final var requires = requiresOpt.get();
        if (layoutThen != requires) {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            new RCCPortImageLayout(
              new Changed(layoutThen, requires),
              new Unchanged(requires)
            )
          );
        } else {
          this.portImageLayoutTransitions.put(
            port.fullPath(),
            unchanged
          );
        }
      } else {
        this.portImageLayoutTransitions.put(
          port.fullPath(),
          unchanged
        );
      }
    }

    private void processOp(
      final RCTOperationDeclaration op)
    {
      this.opPorts.put(op.name(), new HashSet<>());
    }

    /**
     * Create primitive ports for the given port.
     *
     * @param port The port
     */

    private void processCreatePort(
      final RCTPortType port)
      throws RCCompilerException
    {
      switch (port) {
        case final RCTPortConsumer p -> this.processCreateConsumerPorts(p);
        case final RCTPortModifier p -> this.processCreateModifierPorts(p);
        case final RCTPortProducer p -> this.processCreateProducerPorts(p);
      }
    }

    /**
     * Bind resources to each port by tracing their introductions from producer
     * ports.
     */

    private void processBindResources()
    {
      final var iter =
        new TopologicalOrderIterator<>(this.primitiveGraph);

      while (iter.hasNext()) {
        final var port = iter.next();
        for (final var edge : this.primitiveGraph.incomingEdgesOf(port)) {
          final var existing =
            this.portResources.get(edge.sourcePort().fullPath());
          Preconditions.checkPreconditionV(
            existing != null,
            "Port '%s' must have an assigned resource",
            port.fullPath()
          );
          this.portResources.put(port.fullPath(), existing);
        }
      }
    }

    /**
     * Create primitive port connections from the given port connection.
     */

    private void processPortConnection(
      final RCTPortConnection edge)
    {
      final var sourcePrimitives =
        this.portsToPrimitives.get(edge.source());
      final var targetPrimitives =
        this.portsToPrimitives.get(edge.target());

      Preconditions.checkPrecondition(
        sourcePrimitives != null,
        "Source primitives must exist."
      );
      Preconditions.checkPrecondition(
        targetPrimitives != null,
        "Target primitives must exist."
      );
      Preconditions.checkPreconditionV(
        sourcePrimitives.size(),
        sourcePrimitives.size() == targetPrimitives.size(),
        "Source/Target primitive cardinality must match."
      );

      for (final var sourceName : sourcePrimitives.keySet()) {
        final var sourcePrimitive =
          sourcePrimitives.get(sourceName);
        final var targetName =
          new RCCPortPath(edge.targetOperation(), sourceName.path());

        Preconditions.checkPreconditionV(
          targetName,
          targetPrimitives.containsKey(targetName),
          "Target primitive must exist."
        );

        final var targetPrimitive =
          targetPrimitives.get(targetName);

        this.primitiveGraph.addEdge(
          sourcePrimitive,
          targetPrimitive,
          new RCCPortPrimitiveConnection(sourcePrimitive, targetPrimitive)
        );
      }
    }

    private void processCreateModifierPorts(
      final RCTPortModifier p)
    {
      final var t =
        RCTTypeDeclarations.primitiveTreeOf(p.type());

      switch (t) {
        case final RCTPTreeBranchedType br -> {
          for (final var entry : br.nodes().entrySet()) {
            if (entry.getValue() instanceof final RCTPTreeLeafType leaf) {
              final var fullPath =
                fullPathOf(p.owner(), entry.getKey());
              final var prim =
                new RCCPortPrimitiveModifier(
                  p,
                  fullPath,
                  leaf.resource()
                );

              this.primitiveGraph.addVertex(prim);
              this.portPrimitiveMap(p).put(fullPath, prim);
              this.portOpMap(p.owner()).add(prim);
            }
          }
        }

        case final RCTPTreeSingletonType s -> {
          final var fullPath =
            fullPathOf(p.owner(), RCCPath.singleton(p.name()));
          final var prim =
            new RCCPortPrimitiveModifier(p, fullPath, s.resource());

          this.primitiveGraph.addVertex(prim);
          this.portPrimitiveMap(p).put(fullPath, prim);
          this.portOpMap(p.owner()).add(prim);
        }
      }
    }

    private HashMap<RCCPortPath, RCCPortPrimitiveType> portPrimitiveMap(
      final RCTPortType p)
    {
      return this.portsToPrimitives.computeIfAbsent(p, _ -> new HashMap<>());
    }

    private void processCreateProducerPorts(
      final RCTPortProducer p)
      throws RCCompilerException
    {
      final var t =
        RCTTypeDeclarations.primitiveTreeOf(p.type());

      switch (t) {
        case final RCTPTreeBranchedType br -> {
          for (final var entry : br.nodes().entrySet()) {
            if (entry.getValue() instanceof final RCTPTreeLeafType leaf) {
              final var fullPath =
                fullPathOf(p.owner(), entry.getKey());
              final var prim =
                new RCCPortPrimitiveProducer(p, fullPath, leaf.resource());

              this.primitiveGraph.addVertex(prim);
              this.portPrimitiveMap(p).put(fullPath, prim);
              this.portOpMap(p.owner()).add(prim);

              final var h =
                this.freshPlaceholderFor(leaf.resource());
              this.portResources.put(prim.fullPath(), h);
            }
          }
        }

        case final RCTPTreeSingletonType s -> {
          this.checkProducerConstraints(p, s);

          final var fullPath =
            fullPathOf(p.owner(), RCCPath.singleton(p.name()));
          final var prim =
            new RCCPortPrimitiveProducer(p, fullPath, s.resource());

          this.primitiveGraph.addVertex(prim);
          this.portPrimitiveMap(p).put(fullPath, prim);
          this.portOpMap(p.owner()).add(prim);

          final var h =
            this.freshPlaceholderFor(s.resource());
          this.portResources.put(prim.fullPath(), h);
        }
      }
    }

    private void checkProducerConstraints(
      final RCTPortProducer p,
      final RCTPTreeSingletonType s)
      throws RCCompilerException
    {
      if (!s.resource().isImageType()) {
        return;
      }

      final var access = (RCTAccessSetSingletonType) p.accessSet();
      if (access.ensuresImageLayout().isEmpty()) {
        throw RCCompilerException.exceptionOf(
          this.errorImageProducerDoesNotEnsure(p)
        );
      }
    }

    private SStructuredError<String> errorImageProducerDoesNotEnsure(
      final RCTPortProducer port)
    {
      final var position = port.lexical();
      return new SStructuredError<>(
        "error-port-producer-no-ensures",
        "Producer ports for image types must ensure an image layout.",
        Map.ofEntries(
          Map.entry("Operation", port.owner().name().value()),
          Map.entry("Port", port.name().value()),
          Map.entry("Position", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    private void processCreateConsumerPorts(
      final RCTPortConsumer p)
    {
      final var t =
        RCTTypeDeclarations.primitiveTreeOf(p.type());

      switch (t) {
        case final RCTPTreeBranchedType br -> {
          for (final var entry : br.nodes().entrySet()) {
            if (entry.getValue() instanceof final RCTPTreeLeafType leaf) {
              final var fullPath =
                fullPathOf(p.owner(), entry.getKey());
              final var prim =
                new RCCPortPrimitiveConsumer(
                  p,
                  fullPath,
                  leaf.resource()
                );

              this.primitiveGraph.addVertex(prim);
              this.portPrimitiveMap(p).put(fullPath, prim);
              this.portOpMap(p.owner()).add(prim);
            }
          }
        }

        case final RCTPTreeSingletonType _ -> {
          final var fullPath =
            fullPathOf(p.owner(), RCCPath.singleton(p.name()));
          final var prim =
            new RCCPortPrimitiveConsumer(
              p,
              fullPath,
              (RCTPrimitiveResourceType) p.type()
            );

          this.primitiveGraph.addVertex(prim);
          this.portPrimitiveMap(p).put(fullPath, prim);
          this.portOpMap(p.owner()).add(prim);
        }
      }
    }

    private Set<RCCPortPrimitiveType> portOpMap(
      final RCTOperationDeclaration owner)
    {
      return this.opPorts.computeIfAbsent(owner.name(), _ -> new HashSet<>());
    }
  }
}
