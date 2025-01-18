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


package com.io7m.rocaro.rgraphc.internal.checker;

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPackageName;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.loader.RCCLoaderFactoryType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclaration;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortConnection;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortConsumer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortModifier;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortProducer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortSourceType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortTargetType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPortType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPorts;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPrimitiveResourceType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationBuffer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationImage;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationRecord;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationRenderTarget;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclBufferType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclConnect;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutForAllImages;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutForImage;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclGraph;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclImageType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclOperation;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclPortConsumer;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclPortModifier;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclPortProducer;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclPortType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRecordType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRenderTargetType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutForAllImages;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutForImage;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUImport;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryReadsAll;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryReadsSpecific;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryWritesAll;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryWritesSpecific;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUTypeDeclarationType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUTypeReference;
import com.io7m.seltzer.api.SStructuredError;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.rocaro.rgraphc.internal.RCCPLexical.showPosition;
import static com.io7m.rocaro.rgraphc.internal.RCCompilerException.exceptionOf;

public final class RCCChecker implements RCCCheckerType
{
  private final RCCLoaderFactoryType loaders;
  private final HashMap<RCCPackageName, RCTGraphDeclarationType> imports;
  private final ArrayList<SStructuredError<String>> errors;
  private final HashMap<RCCName, RCTDeclarationType> declarations;
  private final ArrayList<RCCName> typesChecking;
  private final DirectedAcyclicGraph<RCTPortType, RCTPortConnection> portGraph;
  private RCTGraphDeclaration.Builder graphBuilder;
  private RCUDeclGraph graphUntyped;

  public RCCChecker(
    final RCCLoaderFactoryType inLoaders)
  {
    this.loaders =
      Objects.requireNonNull(inLoaders, "loaders");
    this.imports =
      new HashMap<>();
    this.errors =
      new ArrayList<>();
    this.declarations =
      new HashMap<>();
    this.typesChecking =
      new ArrayList<>();
    this.portGraph =
      new DirectedAcyclicGraph<>(RCTPortConnection.class);
  }

  private static <A, B> Stream<B> filterInstanceOf(
    final Stream<A> stream,
    final Class<B> clazz)
  {
    return stream.filter(x -> clazz.isAssignableFrom(x.getClass()))
      .map(clazz::cast);
  }

  private static SStructuredError<String> errorNonexistentSubresource(
    final RCTTypeDeclarationType type,
    final RCCPath path,
    final LexicalPosition<URI> position)
  {
    return new SStructuredError<>(
      "error-subresource-nonexistent",
      "Nonexistent subresource.",
      Map.ofEntries(
        Map.entry("Type Package", type.packageName().value()),
        Map.entry("Type", type.name().value()),
        Map.entry("Subresource Path", path.toString()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorNonexistentDeclaration(
    final RCUTypeReference type)
  {
    final var packName =
      type.packageName()
        .map(RCCPackageName::value)
        .orElse(this.graphBuilder.name().value());

    return new SStructuredError<>(
      "error-type-nonexistent",
      "Nonexistent type declaration.",
      Map.ofEntries(
        Map.entry("Type Package", packName),
        Map.entry("Type", type.type().value()),
        Map.entry("Position", showPosition(type.lexical()))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  @Override
  public RCTGraphDeclarationType execute(
    final RCUDeclGraph graph)
    throws RCCompilerException
  {
    this.graphUntyped = graph;
    this.createGraph();
    this.checkImports();
    this.checkErrors();

    this.checkTypeDeclarations();
    this.checkOperations();
    this.checkConnects();
    this.checkErrors();
    return this.graphBuilder.build(this.portGraph);
  }

  private void checkConnects()
  {
    final var connections =
      filterInstanceOf(
        this.graphUntyped.elements().stream(),
        RCUDeclConnect.class
      ).toList();

    for (final var connect : connections) {
      try {
        this.checkConnect(connect);
      } catch (final RCCompilerException e) {
        this.errors.addAll(e.errors());
      }
    }

    this.checkPortsConnected();
  }

  private void checkPortsConnected()
  {
    for (final var port : this.portGraph.vertexSet()) {
      try {
        this.checkPortConnected(port);
      } catch (final RCCompilerException e) {
        this.errors.addAll(e.errors());
      }
    }
  }

  private void checkPortConnected(
    final RCTPortType port)
    throws RCCompilerException
  {
    switch (port) {
      case final RCTPortConsumer c -> {
        if (this.portGraph.degreeOf(c) != 1) {
          throw exceptionOf(this.errorPortConsumerOneConnection(c));
        }
      }
      case final RCTPortProducer p -> {
        if (this.portGraph.degreeOf(p) != 1) {
          throw exceptionOf(this.errorPortProducerOneConnection(p));
        }
      }
      case final RCTPortModifier m -> {
        if (this.portGraph.degreeOf(m) != 2) {
          throw exceptionOf(this.errorPortModifierOneConnection(m));
        }
      }
    }
  }

  private SStructuredError<String> errorPortModifierOneConnection(
    final RCTPortModifier port)
  {
    final var position = port.lexical();
    return new SStructuredError<>(
      "error-port-modifier-connections",
      "Modifier ports must have exactly one incoming and one outgoing connection.",
      Map.ofEntries(
        Map.entry("Operation", port.owner().name().value()),
        Map.entry("Port", port.name().value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorPortProducerOneConnection(
    final RCTPortProducer port)
  {
    final var position = port.lexical();
    return new SStructuredError<>(
      "error-port-producer-one-connection",
      "Producer ports must have exactly one connection.",
      Map.ofEntries(
        Map.entry("Operation", port.owner().name().value()),
        Map.entry("Port", port.name().value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorPortConsumerOneConnection(
    final RCTPortConsumer port)
  {
    final var position = port.lexical();
    return new SStructuredError<>(
      "error-port-consumer-one-connection",
      "Consumer ports must have exactly one connection.",
      Map.ofEntries(
        Map.entry("Operation", port.owner().name().value()),
        Map.entry("Port", port.name().value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private void checkConnect(
    final RCUDeclConnect connect)
    throws RCCompilerException
  {
    final var source =
      this.declarations.get(connect.sourceOperation());
    final var target =
      this.declarations.get(connect.targetOperation());

    if (!(source instanceof final RCTOperationDeclaration sourceOp)) {
      throw exceptionOf(
        this.errorNonexistentOperation(
          connect.lexical(),
          connect.sourceOperation()
        )
      );
    }
    if (!(target instanceof final RCTOperationDeclaration targetOp)) {
      throw exceptionOf(
        this.errorNonexistentOperation(
          connect.lexical(),
          connect.targetOperation()
        )
      );
    }

    final var portSource =
      this.lookupPortSource(connect, sourceOp);
    final var portTarget =
      this.lookupPortTarget(connect, targetOp);

    final var typeSource =
      portSource.type();
    final var typeTarget = portTarget.type();

    final var portSourceName =
      typeSource.fullName();
    final var portTargetName =
      typeTarget.fullName();

    if (!Objects.equals(portSourceName, portTargetName)) {
      throw exceptionOf(
        this.errorPortTypeIncompatible(
          connect.lexical(),
          connect.sourceOperation(),
          connect.sourcePort(),
          typeSource,
          connect.targetOperation(),
          connect.targetPort(),
          typeTarget
        )
      );
    }

    this.portGraph.addVertex(portSource);
    this.portGraph.addVertex(portTarget);

    this.checkPortCardinalityForConnect(portSource);
    this.checkPortCardinalityForConnect(portTarget);
    this.checkConnectMakeEdge(
      connect,
      portSource,
      portTarget,
      typeSource,
      typeTarget
    );
  }

  private void checkPortCardinalityForConnect(
    final RCTPortType port)
    throws RCCompilerException
  {
    switch (port) {
      case final RCTPortModifier m -> {
        if (this.portGraph.degreeOf(m) >= 2) {
          throw exceptionOf(this.errorPortConnected(port.lexical(), port));
        }
      }
      case final RCTPortConsumer c -> {
        if (this.portGraph.degreeOf(c) >= 1) {
          throw exceptionOf(this.errorPortConnected(port.lexical(), port));
        }
      }
      case final RCTPortProducer p -> {
        if (this.portGraph.degreeOf(p) >= 1) {
          throw exceptionOf(this.errorPortConnected(port.lexical(), port));
        }
      }
    }
  }

  private SStructuredError<String> errorPortConnected(
    final LexicalPosition<URI> position,
    final RCTPortType port)
  {
    return new SStructuredError<>(
      "error-port-already-connected",
      "The specified port is already connected.",
      Map.ofEntries(
        Map.entry("Operation", port.owner().name().value()),
        Map.entry("Port", port.name().value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private void checkConnectMakeEdge(
    final RCUDeclConnect connect,
    final RCTPortSourceType portSource,
    final RCTPortTargetType portTarget,
    final RCTTypeDeclarationType typeSource,
    final RCTTypeDeclarationType typeTarget)
    throws RCCompilerException
  {
    try {
      this.portGraph.addEdge(
        portSource,
        portTarget,
        new RCTPortConnection(portSource, portTarget)
      );
    } catch (final IllegalArgumentException e) {
      throw exceptionOf(
        this.errorPortCyclic(
          connect.lexical(),
          connect.sourceOperation(),
          connect.sourcePort(),
          typeSource,
          connect.targetOperation(),
          connect.targetPort(),
          typeTarget
        )
      );
    }
  }

  private SStructuredError<String> errorPortCyclic(
    final LexicalPosition<URI> position,
    final RCCName sourceOp,
    final RCCName sourcePort,
    final RCTTypeDeclarationType typeSource,
    final RCCName targetOp,
    final RCCName targetPort,
    final RCTTypeDeclarationType typeTarget)
  {
    return new SStructuredError<>(
      "error-port-cyclic",
      "Connecting the specified ports would introduce a cycle in the graph.",
      Map.ofEntries(
        Map.entry("Source Operation", sourceOp.value()),
        Map.entry("Source Port", sourcePort.value()),
        Map.entry("Source Type", typeSource.fullName()),
        Map.entry("Target Operation", targetOp.value()),
        Map.entry("Target Port", targetPort.value()),
        Map.entry("Target Type", typeTarget.fullName()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorPortTypeIncompatible(
    final LexicalPosition<URI> position,
    final RCCName sourceOp,
    final RCCName sourcePort,
    final RCTTypeDeclarationType typeSource,
    final RCCName targetOp,
    final RCCName targetPort,
    final RCTTypeDeclarationType typeTarget)
  {
    return new SStructuredError<>(
      "error-port-type-incompatible",
      "The specified ports are type-incompatible and cannot be connected.",
      Map.ofEntries(
        Map.entry("Source Operation", sourceOp.value()),
        Map.entry("Source Port", sourcePort.value()),
        Map.entry("Source Type", typeSource.fullName()),
        Map.entry("Target Operation", targetOp.value()),
        Map.entry("Target Port", targetPort.value()),
        Map.entry("Target Type", typeTarget.fullName()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private RCTPortTargetType lookupPortTarget(
    final RCUDeclConnect connect,
    final RCTOperationDeclaration operation)
    throws RCCompilerException
  {
    final var portT =
      operation.ports().get(connect.targetPort());

    if (portT == null) {
      throw exceptionOf(
        this.errorNonexistentPort(
          connect.lexical(),
          operation,
          connect.targetPort()
        )
      );
    }

    if (portT instanceof final RCTPortTargetType portTarget) {
      return portTarget;
    }

    throw exceptionOf(
      this.errorPortNotTarget(
        connect.lexical(),
        operation,
        connect.sourcePort()
      )
    );
  }

  private RCTPortSourceType lookupPortSource(
    final RCUDeclConnect connect,
    final RCTOperationDeclaration sourceOp)
    throws RCCompilerException
  {
    final var portS =
      sourceOp.ports().get(connect.sourcePort());

    if (portS == null) {
      throw exceptionOf(
        this.errorNonexistentPort(
          connect.lexical(),
          sourceOp,
          connect.sourcePort()
        )
      );
    }

    if (portS instanceof final RCTPortSourceType portSource) {
      return portSource;
    }

    throw exceptionOf(
      this.errorPortNotSource(
        connect.lexical(),
        sourceOp,
        connect.sourcePort()
      )
    );
  }

  private SStructuredError<String> errorPortNotTarget(
    final LexicalPosition<URI> position,
    final RCTOperationDeclaration operation,
    final RCCName portName)
  {
    return new SStructuredError<>(
      "error-port-not-target",
      "The specified port is not a target port.",
      Map.ofEntries(
        Map.entry("Operation", operation.name().value()),
        Map.entry("Port", portName.value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorPortNotSource(
    final LexicalPosition<URI> position,
    final RCTOperationDeclaration operation,
    final RCCName portName)
  {
    return new SStructuredError<>(
      "error-port-not-source",
      "The specified port is not a source port.",
      Map.ofEntries(
        Map.entry("Operation", operation.name().value()),
        Map.entry("Port", portName.value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorNonexistentPort(
    final LexicalPosition<URI> position,
    final RCTOperationDeclaration operation,
    final RCCName portName)
  {
    return new SStructuredError<>(
      "error-port-nonexistent",
      "The specified port does not exist.",
      Map.ofEntries(
        Map.entry("Operation", operation.name().value()),
        Map.entry("Port", portName.value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorNonexistentOperation(
    final LexicalPosition<URI> position,
    final RCCName name)
  {
    return new SStructuredError<>(
      "error-operation-nonexistent",
      "The specified operation does not exist.",
      Map.ofEntries(
        Map.entry("Operation", name.value()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private void checkOperations()
  {
    final var operations =
      filterInstanceOf(
        this.graphUntyped.elements().stream(),
        RCUDeclOperation.class
      ).toList();

    for (final var operation : operations) {
      try {
        this.checkOperation(operation);
      } catch (final RCCompilerException e) {
        this.errors.addAll(e.errors());
      }
    }
  }

  private void checkOperation(
    final RCUDeclOperation operation)
    throws RCCompilerException
  {
    final var op =
      this.graphBuilder.addDeclarationOperation(
        operation.lexical(),
        operation.name(),
        operation.queueCategory(),
        b -> {
          for (final var entry : operation.ports().entrySet()) {
            this.checkOperationPort(b, entry.getValue());
          }
        });

    for (final var port : op.ports().values()) {
      this.portGraph.addVertex(port);
    }

    this.declarations.put(operation.name(), op);
  }

  private void checkOperationPort(
    final RCTOperationDeclaration.Builder b,
    final RCUDeclPortType portDecl)
    throws RCCompilerException
  {
    switch (portDecl) {
      case final RCUDeclPortConsumer c -> {
        b.createConsumerPort(
          c.lexical(),
          c.name(),
          this.resolveOrCheckType(c.type()),
          bp -> this.configurePortForType(c, bp)
        );
      }

      case final RCUDeclPortModifier m -> {
        b.createModifierPort(
          m.lexical(),
          m.name(),
          this.resolveOrCheckType(m.type()),
          bp -> this.configurePortForType(m, bp)
        );
      }

      case final RCUDeclPortProducer p -> {
        b.createProducerPort(
          p.lexical(),
          p.name(),
          this.resolveOrCheckType(p.type()),
          bp -> this.configurePortForType(p, bp)
        );
      }
    }
  }

  private void configurePortForType(
    final RCUDeclPortType port,
    final RCTPorts.AbstractBuilder builder)
    throws RCCompilerException
  {
    final var type = builder.type();

    for (final var r : port.readsAt()) {
      switch (r) {
        case final RCUMemoryReadsAll all -> {
          builder.addReadsAll(all.stage());
        }

        case final RCUMemoryReadsSpecific specific -> {
          this.resolveTypeField(type, specific.path());

          builder.addReadsSpecific(
            specific.path(),
            specific.stage()
          );
        }
      }
    }

    for (final var w : port.writesAt()) {
      switch (w) {
        case final RCUMemoryWritesAll all -> {
          builder.addWritesAll(all.stage());
        }

        case final RCUMemoryWritesSpecific specific -> {
          this.resolveTypeField(type, specific.path());
          builder.addWritesSpecific(
            specific.path(),
            specific.stage()
          );
        }
      }
    }

    for (final var i : port.ensuresImageLayout()) {
      switch (i) {
        case final RCUDeclEnsuresImageLayoutForAllImages all -> {
          builder.addEnsuresImageLayoutForAllImages(all.layout());
        }

        case final RCUDeclEnsuresImageLayoutForImage specific -> {
          final var sub =
            this.resolveTypeField(type, specific.image());

          if (!sub.isImageType()) {
            throw exceptionOf(
              this.errorSubresourceMustBeImageType(
                type,
                specific.lexical(),
                sub
              )
            );
          }

          builder.addEnsuresImageLayoutSpecific(
            specific.image(),
            specific.layout()
          );
        }
      }
    }

    for (final var i : port.requiresImageLayout()) {
      switch (i) {
        case final RCUDeclRequiresImageLayoutForAllImages all -> {
          builder.addRequiresImageLayoutForAllImages(all.layout());
        }

        case final RCUDeclRequiresImageLayoutForImage specific -> {
          final var sub =
            this.resolveTypeField(type, specific.image());

          if (!sub.isImageType()) {
            throw exceptionOf(
              this.errorSubresourceMustBeImageType(
                type,
                specific.lexical(),
                sub
              )
            );
          }

          builder.addRequiresImageLayoutSpecific(
            specific.image(),
            specific.layout()
          );
        }
      }
    }
  }

  private SStructuredError<String> errorSubresourceMustBeImageType(
    final RCTTypeDeclarationType type,
    final LexicalPosition<URI> position,
    final RCTPrimitiveResourceType subresource)
  {
    return new SStructuredError<>(
      "error-primitive-must-be-image",
      "The specified primitive resource must be an image type.",
      Map.ofEntries(
        Map.entry("Type Package", type.packageName().value()),
        Map.entry("Type", type.name().value()),
        Map.entry("Subresource", subresource.toString()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorNotASubresource(
    final RCTTypeDeclarationType type,
    final RCCPath path,
    final LexicalPosition<URI> position)
  {
    return new SStructuredError<>(
      "error-not-primitive-resource",
      "The object at the specified path is not a primitive resource.",
      Map.ofEntries(
        Map.entry("Type Package", type.packageName().value()),
        Map.entry("Type", type.name().value()),
        Map.entry("Subresource", path.toString()),
        Map.entry("Position", showPosition(position))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private RCTPrimitiveResourceType resolveTypeField(
    final RCTTypeDeclarationType type,
    final RCCPath path)
    throws RCCompilerException
  {
    final var position =
      type.lexical();
    final var pathRemainingOpt =
      path.tail();

    return switch (type) {
      case final RCTTypeDeclarationBuffer _,
           final RCTTypeDeclarationImage _ -> {
        throw exceptionOf(errorNonexistentSubresource(type, path, position));
      }

      case final RCTTypeDeclarationRenderTarget rt -> {
        if (pathRemainingOpt.isPresent()) {
          throw exceptionOf(errorNonexistentSubresource(type, path, position));
        }

        final var name =
          path.head();
        final var subresource =
          rt.allAttachments().get(name);

        if (subresource == null) {
          throw exceptionOf(errorNonexistentSubresource(type, path, position));
        }

        yield subresource;
      }

      case final RCTTypeDeclarationRecord r -> {
        if (pathRemainingOpt.isPresent()) {
          final var pathRemaining = pathRemainingOpt.get();
          yield this.resolveTypeField(r, pathRemaining);
        }

        final var name =
          path.head();

        final var subresource =
          r.fields().get(name);

        if (subresource == null) {
          throw exceptionOf(errorNonexistentSubresource(type, path, position));
        }

        yield switch (subresource.type()) {
          case final RCTTypeDeclarationBuffer b -> {
            yield b;
          }
          case final RCTTypeDeclarationImage i -> {
            yield i;
          }
          case final RCTTypeDeclarationRecord _,
               final RCTTypeDeclarationRenderTarget _ -> {
            throw exceptionOf(this.errorNotASubresource(type, path, position));
          }
        };
      }
    };
  }

  private RCTTypeDeclarationType resolveOrCheckType(
    final RCUTypeReference type)
    throws RCCompilerException
  {
    final var packNameOpt = type.packageName();
    if (packNameOpt.isPresent()) {
      final var packName = packNameOpt.get();
      if (!Objects.equals(packName, this.graphBuilder.name())) {
        return this.resolveOrCheckTypeInOtherPackage(type);
      }
    }

    final var declaration =
      this.declarations.get(type.type());

    return switch (declaration) {
      case final RCTOperationDeclaration _ -> {
        throw exceptionOf(this.errorDeclarationNotType(type));
      }
      case final RCTTypeDeclarationType typeDeclaration -> {
        yield typeDeclaration;
      }
      case null -> {
        final var untyped =
          filterInstanceOf(
            this.graphUntyped.elements().stream(),
            RCUTypeDeclarationType.class)
            .filter(u -> Objects.equals(u.name(), type.type()))
            .findFirst();

        if (untyped.isEmpty()) {
          throw exceptionOf(this.errorNonexistentDeclaration(type));
        }

        yield this.checkTypeDeclaration(untyped.get());
      }
    };
  }

  private SStructuredError<String> errorDeclarationNotType(
    final RCUTypeReference type)
  {
    final var packName =
      type.packageName()
        .map(RCCPackageName::value)
        .orElse(this.graphBuilder.name().value());

    return new SStructuredError<>(
      "error-declaration-not-type",
      "The referenced declaration is not a type.",
      Map.ofEntries(
        Map.entry("Target Package", packName),
        Map.entry("Target Declaration", type.type().value()),
        Map.entry("Position", showPosition(type.lexical()))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private SStructuredError<String> errorPackageNotImported(
    final RCUTypeReference type)
  {
    final var packName =
      type.packageName()
        .map(RCCPackageName::value)
        .orElseThrow();

    return new SStructuredError<>(
      "error-package-not-imported",
      "The referenced package is not imported.",
      Map.ofEntries(
        Map.entry("Target Package", packName),
        Map.entry("Target Declaration", type.type().value()),
        Map.entry("Position", showPosition(type.lexical()))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private RCTTypeDeclarationType resolveOrCheckTypeInOtherPackage(
    final RCUTypeReference type)
    throws RCCompilerException
  {
    final var packName =
      type.packageName().orElseThrow();

    final var other =
      this.imports.get(packName);

    if (other == null) {
      throw exceptionOf(this.errorPackageNotImported(type));
    }

    final var declaration =
      other.declarations()
        .get(type.type());

    if (declaration == null) {
      throw exceptionOf(this.errorNonexistentDeclaration(type));
    }

    return switch (declaration) {
      case final RCTOperationDeclaration _ -> {
        throw exceptionOf(this.errorDeclarationNotType(type));
      }
      case final RCTTypeDeclarationType t -> {
        yield t;
      }
    };
  }

  private void createGraph()
  {
    this.graphBuilder =
      RCTGraphDeclaration.builder(this.graphUntyped.packageName());
  }

  private void checkTypeDeclarations()
  {
    final var types =
      filterInstanceOf(
        this.graphUntyped.elements().stream(),
        RCUTypeDeclarationType.class
      ).toList();

    for (final var type : types) {
      try {
        if (!this.declarations.containsKey(type.name())) {
          this.checkTypeDeclaration(type);
        }
      } catch (final RCCompilerException e) {
        this.errors.addAll(e.errors());
      }
    }
  }

  private RCTTypeDeclarationType checkTypeDeclaration(
    final RCUTypeDeclarationType type)
    throws RCCompilerException
  {
    if (this.typesChecking.contains(type.name())) {
      throw exceptionOf(this.errorCircularDependency(type));
    }

    try {
      this.typesChecking.addLast(type.name());

      return switch (type) {
        case final RCUDeclBufferType b -> {
          yield this.checkTypeDeclarationBuffer(b);
        }
        case final RCUDeclImageType i -> {
          yield this.checkTypeDeclarationImage(i);
        }
        case final RCUDeclRenderTargetType rt -> {
          yield this.checkTypeDeclarationRenderTarget(rt);
        }
        case final RCUDeclRecordType r -> {
          yield this.checkTypeDeclarationRecord(r);
        }
      };
    } finally {
      this.typesChecking.removeLast();
    }
  }

  private SStructuredError<String> errorCircularDependency(
    final RCUTypeDeclarationType type)
  {
    return new SStructuredError<>(
      "error-declaration-circular",
      "The type declaration(s) form a circular dependency.",
      Map.ofEntries(
        Map.entry("Package", this.graphBuilder.name().value()),
        Map.entry("Type Declaration", type.name().value()),
        Map.entry("Circular References", this.typeCheckingPath()),
        Map.entry("Position", showPosition(type.lexical()))
      ),
      Optional.empty(),
      Optional.empty()
    );
  }

  private String typeCheckingPath()
  {
    return this.typesChecking.stream()
      .map(RCCName::value)
      .collect(Collectors.joining(" → "));
  }

  private RCTTypeDeclarationRecord checkTypeDeclarationRecord(
    final RCUDeclRecordType r)
    throws RCCompilerException
  {
    final var type =
      this.graphBuilder.addTypeDeclarationRecord(
        r.lexical(),
        r.name(),
        b -> {
          for (final var f : r.fields()) {
            b.addField(
              f.lexical(),
              f.name(),
              this.resolveOrCheckType(f.type())
            );
          }
        });

    this.declarations.put(r.name(), type);
    return type;
  }

  private RCTTypeDeclarationRenderTarget checkTypeDeclarationRenderTarget(
    final RCUDeclRenderTargetType rt)
    throws RCCompilerException
  {
    final var type =
      this.graphBuilder.addTypeDeclarationRenderTarget(
        rt.lexical(),
        rt.name(),
        b -> {
          for (final var c : rt.colorAttachments()) {
            b.addColorAttachment(c.lexical(), c.name(), c.index());
          }

          final var depthOpt =
            rt.depthAttachment();

          if (depthOpt.isPresent()) {
            final var attachment = depthOpt.get();
            b.setDepthAttachment(attachment.lexical(), attachment.name());
          }
        });

    this.declarations.put(rt.name(), type);
    return type;
  }

  private RCTTypeDeclarationImage checkTypeDeclarationImage(
    final RCUDeclImageType i)
    throws RCCompilerException
  {
    final var type =
      this.graphBuilder.addTypeDeclarationImage(i.lexical(), i.name());

    this.declarations.put(i.name(), type);
    return type;
  }

  private RCTTypeDeclarationBuffer checkTypeDeclarationBuffer(
    final RCUDeclBufferType b)
    throws RCCompilerException
  {
    final var type =
      this.graphBuilder.addTypeDeclarationBuffer(b.lexical(), b.name());

    this.declarations.put(b.name(), type);
    return type;
  }

  private void checkErrors()
    throws RCCompilerException
  {
    if (!this.errors.isEmpty()) {
      final var first =
        this.errors.get(0);

      throw new RCCompilerException(
        first.message(),
        first.attributes(),
        first.errorCode(),
        first.remediatingAction(),
        this.errors
      );
    }
  }

  private void checkImports()
  {
    final var importList =
      filterInstanceOf(this.graphUntyped.elements().stream(), RCUImport.class)
        .toList();

    for (final var importE : importList) {
      try {
        final var loader =
          this.loaders.createLoader();
        final var loaded =
          loader.load(importE.packageName());

        this.imports.put(importE.packageName(), loaded);
      } catch (final RCCompilerException e) {
        this.errors.addAll(e.errors());
      }
    }
  }
}
