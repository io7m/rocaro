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


package com.io7m.rocaro.rgraphc.internal.primitive_tree;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.typed.RCTPrimitiveResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

public final class RCTPrimitiveTree
{
  private RCTPrimitiveTree()
  {

  }

  public static RCTPTreeSingletonType singleton(
    final RCTPrimitiveResourceType resource)
  {
    return new Singleton(resource);
  }

  public static Builder builder()
  {
    return new Builder();
  }

  public static final class Builder
  {
    private final RootWithChildren root;
    private boolean built;

    Builder()
    {
      this.built = false;
      this.root = new RootWithChildren();
    }

    public RCTPTreeBranchedType build()
    {
      this.checkNotBuilt();
      return this.root;
    }

    public BranchBuilder addBranch(
      final RCCName name)
    {
      Objects.requireNonNull(name, "name");
      this.checkNotBuilt();

      if (this.root.children.containsKey(name)) {
        throw new IllegalArgumentException(
          "Name %s already used.".formatted(name)
        );
      }

      final var path = new RCCPath(List.of(name));
      Preconditions.checkPreconditionV(
        !this.root.allNodes.containsKey(path),
        "Node path must not exist"
      );

      final var branch = new Branch(this.root, path, name);
      this.root.children.put(name, branch);
      this.root.allNodes.put(path, branch);
      return new BranchBuilder(this, branch);
    }

    public Builder addResource(
      final RCCName name,
      final RCTPrimitiveResourceType resource)
    {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(resource, "resource");
      this.checkNotBuilt();

      if (this.root.children.containsKey(name)) {
        throw new IllegalArgumentException(
          "Name %s already used.".formatted(name)
        );
      }

      final var path = new RCCPath(List.of(name));
      Preconditions.checkPreconditionV(
        !this.root.allNodes.containsKey(path),
        "Node path must not exist"
      );

      final var node = new Leaf(this.root, path, name, resource);
      this.root.children.put(name, node);
      this.root.allNodes.put(path, node);
      return this;
    }

    private void checkNotBuilt()
    {
      if (this.built) {
        throw new IllegalStateException("Builder already completed.");
      }
    }
  }

  public static final class BranchBuilder
  {
    private final Builder builder;
    private final Branch branch;

    BranchBuilder(
      final Builder inBuilder,
      final Branch inBranch)
    {
      this.builder =
        Objects.requireNonNull(inBuilder, "builder");
      this.branch =
        Objects.requireNonNull(inBranch, "branch");
    }

    public BranchBuilder addBranch(
      final RCCName name)
    {
      Objects.requireNonNull(name, "name");
      this.builder.checkNotBuilt();

      if (this.branch.children.containsKey(name)) {
        throw new IllegalArgumentException(
          "Name %s already used.".formatted(name)
        );
      }

      final var newPathElements = new ArrayList<>(this.branch.path.value());
      newPathElements.addLast(name);
      final var newPath = new RCCPath(newPathElements);
      final var branch = new Branch(this.branch, newPath, name);

      Preconditions.checkPreconditionV(
        this.builder.root.allNodes.containsKey(newPath),
        "Node path must not exist"
      );

      this.branch.children.put(name, branch);
      this.builder.root.allNodes.put(newPath, branch);
      return new BranchBuilder(this.builder, branch);
    }

    public BranchBuilder addResource(
      final RCCName name,
      final RCTPrimitiveResourceType resource)
    {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(resource, "resource");
      this.builder.checkNotBuilt();

      if (this.branch.children.containsKey(name)) {
        throw new IllegalArgumentException(
          "Name %s already used.".formatted(name)
        );
      }

      final var newPathElements = new ArrayList<>(this.branch.path.value());
      newPathElements.addLast(name);
      final var newPath = new RCCPath(newPathElements);

      Preconditions.checkPreconditionV(
        !this.builder.root.allNodes.containsKey(newPath),
        "Node path must not exist"
      );

      final var node = new Leaf(this.branch, newPath, name, resource);
      this.branch.children.put(name, node);
      this.builder.root.allNodes.put(newPath, node);
      return this;
    }
  }

  private static sealed abstract class Node
  {

  }

  private static sealed abstract class Root
    extends Node
  {

  }

  private static sealed abstract class NonRoot
    extends Node
  {

  }

  private static final class Leaf
    extends NonRoot
    implements RCTPTreeLeafType
  {
    private final Node parent;
    private final RCCName name;
    private final RCTPrimitiveResourceType resource;
    private final RCCPath path;

    public Leaf(
      final Node inParent,
      final RCCPath inPath,
      final RCCName inName,
      final RCTPrimitiveResourceType inResource)
    {
      this.parent =
        Objects.requireNonNull(inParent, "parent");
      this.path =
        Objects.requireNonNull(inPath, "path");
      this.name =
        Objects.requireNonNull(inName, "name");
      this.resource =
        Objects.requireNonNull(inResource, "resource");
    }

    @Override
    public RCCName name()
    {
      return this.name;
    }

    @Override
    public RCTPrimitiveResourceType resource()
    {
      return this.resource;
    }

    @Override
    public RCCPath path()
    {
      return this.path;
    }
  }

  private static final class Branch
    extends NonRoot
    implements RCTPTreeBranchType
  {
    private final Node parent;
    private final RCCName name;
    private final TreeMap<RCCName, NonRoot> children;
    private final RCCPath path;

    public Branch(
      final Node inParent,
      final RCCPath inPath,
      final RCCName inName)
    {
      this.parent =
        Objects.requireNonNull(inParent, "parent");
      this.path =
        Objects.requireNonNull(inPath, "path");
      this.name =
        Objects.requireNonNull(inName, "name");
      this.children =
        new TreeMap<>();
    }

    @Override
    public RCCName name()
    {
      return this.name;
    }

    @Override
    public SortedSet<RCCName> resourceNames()
    {
      return this.children.navigableKeySet();
    }

    @Override
    public RCTPTreeNodeType nodeAt(
      final RCCName name)
    {
      final var node = this.children.get(name);
      if (node == null) {
        throw new IllegalArgumentException("No such name: %s".formatted(name));
      }
      return (RCTPTreeNodeType) node;
    }

    @Override
    public RCCPath path()
    {
      return this.path;
    }
  }

  private static final class RootWithChildren
    extends Root
    implements RCTPTreeBranchedType
  {
    private final TreeMap<RCCName, NonRoot> children;
    private final TreeMap<RCCPath, RCTPTreeNodeType> allNodes;

    public RootWithChildren()
    {
      this.children =
        new TreeMap<>();
      this.allNodes =
        new TreeMap<>();
    }

    @Override
    public SortedSet<RCCName> resourceNames()
    {
      return this.children.navigableKeySet();
    }

    @Override
    public RCTPTreeNodeType nodeAt(
      final RCCName name)
    {
      final var node = this.children.get(name);
      if (node == null) {
        throw new IllegalArgumentException("No such name: %s".formatted(name));
      }
      return (RCTPTreeNodeType) node;
    }

    @Override
    public SortedMap<RCCPath, RCTPTreeNodeType> nodes()
    {
      return Collections.unmodifiableSortedMap(this.allNodes);
    }

    @Override
    public RCTPTreeNodeType nodeForPath(
      final RCCPath path)
    {
      final var node = this.allNodes.get(path);
      if (node == null) {
        throw new IllegalArgumentException("No such node: %s".formatted(path));
      }
      return node;
    }
  }

  private static final class Singleton
    extends Root
    implements RCTPTreeSingletonType
  {
    private final RCTPrimitiveResourceType resource;

    Singleton(
      final RCTPrimitiveResourceType inResource)
    {
      this.resource =
        Objects.requireNonNull(inResource, "resource");
    }

    @Override
    public RCTPrimitiveResourceType resource()
    {
      return this.resource;
    }

    @Override
    public RCTPTreeNodeType nodeForPath(
      final RCCPath path)
    {
      throw new IllegalArgumentException("No such node: %s".formatted(path));
    }

    @Override
    public SortedMap<RCCPath, RCTPTreeNodeType> nodes()
    {
      return Collections.emptySortedMap();
    }
  }
}
