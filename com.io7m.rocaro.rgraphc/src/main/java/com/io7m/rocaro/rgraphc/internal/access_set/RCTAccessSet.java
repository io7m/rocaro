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


package com.io7m.rocaro.rgraphc.internal.access_set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeBranchType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeBranchedType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeLeafType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeSingletonType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationCompositeType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationImage;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationPrimitiveType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarations;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class RCTAccessSet
{
  private RCTAccessSet()
  {

  }

  public static BuilderType builder(
    final RCTTypeDeclarationType type)
  {
    final var tree =
      RCTTypeDeclarations.primitiveTreeOf(type);

    return switch (tree) {
      case final RCTPTreeBranchedType branched -> {
        Preconditions.checkPrecondition(
          type instanceof RCTTypeDeclarationCompositeType,
          "Type must be a composite type."
        );
        yield new BuilderComposite(
          branched
        );
      }
      case final RCTPTreeSingletonType singleton -> {
        Preconditions.checkPrecondition(
          type instanceof RCTTypeDeclarationPrimitiveType,
          "Type must be a primitive type."
        );
        yield new BuilderSingleton(
        );
      }
    };
  }

  public interface BuilderType
  {
    void addReadsAll(
      final RCGCommandPipelineStage stage);

    void addReadsSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage);

    void addWritesAll(
      final RCGCommandPipelineStage stage);

    void addWritesSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage);

    void setEnsuresImageLayoutForAllImages(
      final RCGResourceImageLayout layout);

    void setEnsuresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout);

    void setRequiresImageLayoutForAllImages(
      final RCGResourceImageLayout layout);

    void setRequiresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout);

    RCTAccessSetType build();
  }

  public static final class BuilderComposite
    implements BuilderType
  {
    private final RCTPTreeBranchedType tree;
    private final TreeMap<RCCPath, TreeSet<RCGCommandPipelineStage>> primitiveReads;
    private final TreeMap<RCCPath, TreeSet<RCGCommandPipelineStage>> primitiveWrites;
    private final TreeMap<RCCPath, RCGResourceImageLayout> primitiveEnsuresLayout;
    private final TreeMap<RCCPath, RCGResourceImageLayout> primitiveRequiresLayout;
    private boolean built;

    BuilderComposite(
      final RCTPTreeBranchedType inTree)
    {
      this.tree =
        Objects.requireNonNull(inTree, "tree");

      this.primitiveReads =
        new TreeMap<>();
      this.primitiveWrites =
        new TreeMap<>();
      this.primitiveRequiresLayout =
        new TreeMap<>();
      this.primitiveEnsuresLayout =
        new TreeMap<>();
    }

    private void checkNotBuilt()
    {
      if (this.built) {
        throw new IllegalStateException("Builder has already completed.");
      }
    }

    @Override
    public void addReadsAll(
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      for (final var path : this.tree.nodes().keySet()) {
        this.addReadsSpecific(path, stage);
      }
    }

    @Override
    public void addReadsSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      final var node = this.tree.nodeForPath(path);
      switch (node) {
        case final RCTPTreeBranchType br -> {
          for (final var childName : br.resourceNames()) {
            this.addReadsSpecific(path.plus(childName), stage);
          }
        }
        case final RCTPTreeLeafType _ -> {
          this.primitiveReads.computeIfAbsent(path, _ -> new TreeSet<>())
            .add(stage);
        }
      }
    }

    @Override
    public void addWritesAll(
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      for (final var path : this.tree.nodes().keySet()) {
        this.addWritesSpecific(path, stage);
      }
    }

    @Override
    public void addWritesSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      final var node = this.tree.nodeForPath(path);
      switch (node) {
        case final RCTPTreeBranchType br -> {
          for (final var childName : br.resourceNames()) {
            this.addWritesSpecific(path.plus(childName), stage);
          }
        }
        case final RCTPTreeLeafType _ -> {
          this.primitiveWrites.computeIfAbsent(path, _ -> new TreeSet<>())
            .add(stage);
        }
      }
    }

    @Override
    public void setEnsuresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      for (final var entry : this.tree.nodes().entrySet()) {
        if (entry.getValue() instanceof final RCTPTreeLeafType leaf) {
          if (leaf.resource().isImageType()) {
            this.setEnsuresImageLayoutSpecificInner(
              entry.getKey(),
              layout,
              false
            );
          }
        }
      }
    }

    @Override
    public void setEnsuresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      this.setEnsuresImageLayoutSpecificInner(path, layout, true);
    }

    private void setEnsuresImageLayoutSpecificInner(
      final RCCPath path,
      final RCGResourceImageLayout layout,
      final boolean strict)
    {
      final var node =
        this.tree.nodeForPath(path);

      switch (node) {
        case final RCTPTreeBranchType br -> {
          for (final var childName : br.resourceNames()) {
            this.setEnsuresImageLayoutSpecificInner(
              path.plus(childName),
              layout,
              strict
            );
          }
        }
        case final RCTPTreeLeafType leaf -> {
          if (leaf.resource().isImageType()) {
            this.primitiveEnsuresLayout.put(path, layout);
          } else {
            if (strict) {
              throw new IllegalArgumentException(
                "Cannot ensure image layouts for non-image types (Path %s)."
                  .formatted(path)
              );
            }
          }
        }
      }
    }

    @Override
    public void setRequiresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      for (final var entry : this.tree.nodes().entrySet()) {
        if (entry.getValue() instanceof final RCTPTreeLeafType leaf) {
          if (leaf.resource().isImageType()) {
            this.setRequiresImageLayoutSpecificInner(
              entry.getKey(),
              layout,
              false
            );
          }
        }
      }
    }

    private void setRequiresImageLayoutSpecificInner(
      final RCCPath path,
      final RCGResourceImageLayout layout,
      final boolean strict)
    {
      final var node =
        this.tree.nodeForPath(path);

      switch (node) {
        case final RCTPTreeBranchType br -> {
          for (final var childName : br.resourceNames()) {
            this.setEnsuresImageLayoutSpecificInner(
              path.plus(childName),
              layout,
              strict
            );
          }
        }
        case final RCTPTreeLeafType leaf -> {
          if (leaf.resource().isImageType()) {
            this.primitiveRequiresLayout.put(path, layout);
          } else {
            if (strict) {
              throw new IllegalArgumentException(
                "Cannot require image layouts for non-image types (Path %s)."
                  .formatted(path)
              );
            }
          }
        }
      }
    }

    @Override
    public void setRequiresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      this.setRequiresImageLayoutSpecificInner(path, layout, true);
    }

    @Override
    public RCTAccessSetType build()
    {
      this.checkNotBuilt();
      this.built = true;
      return new AccessSetComposite(this);
    }
  }

  public static final class BuilderSingleton
    implements BuilderType
  {
    private final HashSet<RCGCommandPipelineStage> reads;
    private final HashSet<RCGCommandPipelineStage> writes;
    private Optional<RCGResourceImageLayout> ensuresImageLayout;
    private Optional<RCGResourceImageLayout> requiresImageLayout;
    private boolean built;

    BuilderSingleton()
    {
      this.reads =
        new HashSet<>();
      this.writes =
        new HashSet<>();
      this.ensuresImageLayout =
        Optional.empty();
      this.requiresImageLayout =
        Optional.empty();
    }

    @Override
    public void addReadsAll(
      final RCGCommandPipelineStage stage)
    {
      this.checkNotBuilt();
      this.reads.add(Objects.requireNonNull(stage, "stage"));
    }

    private void checkNotBuilt()
    {
      if (this.built) {
        throw new IllegalStateException("Builder has already completed.");
      }
    }

    @Override
    public void addReadsSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      throw new IllegalArgumentException(
        "Primitive resources do not have path-addressable components."
      );
    }

    @Override
    public void addWritesAll(
      final RCGCommandPipelineStage stage)
    {
      this.checkNotBuilt();
      this.writes.add(Objects.requireNonNull(stage, "stage"));
    }

    @Override
    public void addWritesSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(stage, "stage");
      this.checkNotBuilt();

      throw new IllegalArgumentException(
        "Primitive resources do not have path-addressable components."
      );
    }

    @Override
    public void setEnsuresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      this.checkNotBuilt();

      this.ensuresImageLayout =
        Optional.of(Objects.requireNonNull(layout, "layout"));
    }

    @Override
    public void setEnsuresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      throw new IllegalArgumentException(
        "Primitive resources do not have path-addressable components."
      );
    }

    @Override
    public void setRequiresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      this.checkNotBuilt();

      this.requiresImageLayout =
        Optional.of(Objects.requireNonNull(layout, "layout"));
    }

    @Override
    public void setRequiresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(layout, "layout");
      this.checkNotBuilt();

      throw new IllegalArgumentException(
        "Primitive resources do not have path-addressable components."
      );
    }

    @Override
    public RCTAccessSetType build()
    {
      this.checkNotBuilt();

      this.built = true;
      return new AccessSetSingleton(this);
    }
  }

  private static final class AccessSetSingleton
    implements RCTAccessSetSingletonType
  {
    private final BuilderSingleton builder;

    AccessSetSingleton(
      final BuilderSingleton inBuilder)
    {
      this.builder =
        Objects.requireNonNull(inBuilder, "builder");
    }

    @Override
    public String kind()
    {
      return "Singleton";
    }

    @Override
    public Set<RCGCommandPipelineStage> reads()
    {
      return Set.copyOf(this.builder.reads);
    }

    @Override
    public Set<RCGCommandPipelineStage> writes()
    {
      return Set.copyOf(this.builder.writes);
    }

    @Override
    public Optional<RCGResourceImageLayout> ensuresImageLayout()
    {
      return this.builder.ensuresImageLayout;
    }

    @Override
    public Optional<RCGResourceImageLayout> requiresImageLayout()
    {
      return this.builder.requiresImageLayout;
    }
  }

  private static final class AccessSetComposite
    implements RCTAccessSetCompositeType
  {
    private final BuilderComposite builder;

    AccessSetComposite(
      final BuilderComposite inBuilder)
    {
      this.builder =
        Objects.requireNonNull(inBuilder, "builder");
    }

    @JsonProperty("Reads")
    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TreeMap<RCCPath, TreeSet<RCGCommandPipelineStage>> reads()
    {
      return this.builder.primitiveReads;
    }

    @JsonProperty("Writes")
    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TreeMap<RCCPath, TreeSet<RCGCommandPipelineStage>> writes()
    {
      return this.builder.primitiveWrites;
    }

    @JsonProperty("RequiresImageLayout")
    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TreeMap<RCCPath, RCGResourceImageLayout> requiresImageLayout()
    {
      return this.builder.primitiveRequiresLayout;
    }

    @JsonProperty("EnsuresImageLayout")
    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TreeMap<RCCPath, RCGResourceImageLayout> ensuresImageLayout()
    {
      return this.builder.primitiveEnsuresLayout;
    }

    @Override
    public String kind()
    {
      return "Composite";
    }

    @Override
    public Set<RCCPath> paths()
    {
      return this.builder.tree.nodes().keySet();
    }

    @Override
    public Set<RCGCommandPipelineStage> readsFor(
      final RCCPath path)
    {
      final var r = this.builder.primitiveReads.get(path);
      if (r == null) {
        return Set.of();
      }
      return r;
    }

    @Override
    public Set<RCGCommandPipelineStage> writesFor(
      final RCCPath path)
    {
      final var w = this.builder.primitiveWrites.get(path);
      if (w == null) {
        return Set.of();
      }
      return w;
    }

    @Override
    public Optional<RCGResourceImageLayout> ensuresImageLayoutFor(
      final RCCPath path)
    {
      final var i = this.builder.primitiveEnsuresLayout.get(path);
      if (i == null) {
        return Optional.empty();
      }
      return Optional.of(i);
    }

    @Override
    public Optional<RCGResourceImageLayout> requiresImageLayoutFor(
      final RCCPath path)
    {
      final var i = this.builder.primitiveRequiresLayout.get(path);
      if (i == null) {
        return Optional.empty();
      }
      return Optional.of(i);
    }
  }
}
