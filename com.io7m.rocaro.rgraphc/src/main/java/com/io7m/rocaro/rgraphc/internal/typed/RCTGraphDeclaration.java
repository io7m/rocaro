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


package com.io7m.rocaro.rgraphc.internal.typed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPLexical;
import com.io7m.rocaro.rgraphc.internal.RCCPackageName;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.seltzer.api.SStructuredError;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@JsonPropertyOrder(alphabetic = true)
public final class RCTGraphDeclaration
  implements RCTGraphDeclarationType
{
  @JsonProperty("Package")
  private final RCCPackageName name;
  @JsonProperty("Declarations")
  private final TreeMap<RCCName, RCTDeclarationType> declarations;
  private final SortedMap<RCCName, RCTDeclarationType> declarationsRead;
  private Graph<RCTPortType, RCTPortConnection> portGraph;

  @Override
  public String toString()
  {
    return "[RCTGraphDeclaration %s %s]"
      .formatted(this.name, this.declarations);
  }

  private RCTGraphDeclaration(
    final RCCPackageName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.declarations =
      new TreeMap<>();
    this.declarationsRead =
      Collections.unmodifiableSortedMap(this.declarations);
  }

  public static Builder builder(
    final RCCPackageName name)
  {
    return new Builder(name);
  }

  @Override
  public RCCPackageName name()
  {
    return this.name;
  }

  @Override
  public SortedMap<RCCName, RCTDeclarationType> declarations()
  {
    return this.declarationsRead;
  }

  @Override
  public Graph<RCTPortType, RCTPortConnection> portGraph()
  {
    return this.portGraph;
  }

  public static final class Builder
  {
    private RCTGraphDeclaration target;

    Builder(
      final RCCPackageName name)
    {
      this.target =
        new RCTGraphDeclaration(name);
    }

    public RCTGraphDeclarationType build(
      final DirectedAcyclicGraph<RCTPortType, RCTPortConnection> graph)
    {
      this.checkNotBuilt();

      final var r = this.target;
      r.setGraph(graph);
      this.target = null;
      return r;
    }

    private void checkNotBuilt()
    {
      if (this.target == null) {
        throw new IllegalStateException("Builder already completed.");
      }
    }

    public RCTTypeDeclarationImage addTypeDeclarationImage(
      final LexicalPosition<URI> position,
      final RCCName name)
      throws RCCompilerException
    {
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(name, "name");
      this.checkNotBuilt();

      final var r = new RCTTypeDeclarationImage(this.target, name);
      r.setLexical(position);

      this.checkNameNotUsed(position, name);
      this.target.declarations.put(name, r);
      return r;
    }

    private void checkNameNotUsed(
      final LexicalPosition<URI> position,
      final RCCName name)
      throws RCCompilerException
    {
      final var existing =
        this.target.declarations.get(name);

      if (existing != null) {
        throw RCCompilerException.exceptionOf(
          new SStructuredError<>(
            "error-name-used",
            "Name already used.",
            Map.ofEntries(
              Map.entry("Name", name.value()),
              Map.entry("Position (Current)", RCCPLexical.showPosition(position)),
              Map.entry("Position (Existing)", RCCPLexical.showPosition(existing.lexical()))
            ),
            Optional.empty(),
            Optional.empty()
          )
        );
      }
    }

    public RCTTypeDeclarationBuffer addTypeDeclarationBuffer(
      final LexicalPosition<URI> position,
      final RCCName name)
      throws RCCompilerException
    {
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(name, "name");
      this.checkNotBuilt();

      final var r = new RCTTypeDeclarationBuffer(this.target, name);
      r.setLexical(position);

      this.checkNameNotUsed(position, name);
      this.target.declarations.put(name, r);
      return r;
    }

    public RCCPackageName name()
    {
      return this.target.name;
    }

    @FunctionalInterface
    public interface RenderTargetBuilderFunctionType
    {
      void configure(
        RCTTypeDeclarationRenderTarget.Builder b)
        throws RCCompilerException;
    }

    public RCTTypeDeclarationRenderTarget addTypeDeclarationRenderTarget(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RenderTargetBuilderFunctionType f)
      throws RCCompilerException
    {
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(f, "f");
      this.checkNotBuilt();

      final var b =
        RCTTypeDeclarationRenderTarget.builder(this.target, name);

      this.checkNameNotUsed(position, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.declarations.put(name, rr);
      return rr;
    }

    @FunctionalInterface
    public interface RecordBuilderFunctionType
    {
      void configure(
        RCTTypeDeclarationRecord.Builder b)
        throws RCCompilerException;
    }

    public RCTTypeDeclarationRecord addTypeDeclarationRecord(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RecordBuilderFunctionType f)
      throws RCCompilerException
    {
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(f, "f");
      this.checkNotBuilt();

      final var b =
        RCTTypeDeclarationRecord.builder(this.target, name);

      this.checkNameNotUsed(position, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.declarations.put(name, rr);
      return rr;
    }

    @FunctionalInterface
    public interface OperationBuilderFunctionType
    {
      void configure(
        RCTOperationDeclaration.Builder b)
        throws RCCompilerException;
    }

    public RCTOperationDeclaration addDeclarationOperation(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RCDeviceQueueCategory queueCategory,
      final OperationBuilderFunctionType f)
      throws RCCompilerException
    {
      Objects.requireNonNull(position, "position");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(queueCategory, "queueCategory");
      Objects.requireNonNull(f, "f");
      this.checkNotBuilt();

      final var b =
        RCTOperationDeclaration.builder(
          this.target,
          name,
          queueCategory
        );

      this.checkNameNotUsed(position, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.declarations.put(name, rr);
      return rr;
    }
  }

  private void setGraph(
    final DirectedAcyclicGraph<RCTPortType, RCTPortConnection> newGraph)
  {
    this.portGraph = (Graph<RCTPortType, RCTPortConnection>) newGraph.clone();
  }
}
