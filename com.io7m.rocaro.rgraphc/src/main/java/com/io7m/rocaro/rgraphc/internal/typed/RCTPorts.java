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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.access_set.RCTAccessSet;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeBranchType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeBranchedType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeLeafType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeSingletonType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public final class RCTPorts
{
  private RCTPorts()
  {

  }

  public static ConsumerBuilder consumerBuilder(
    final RCTOperationDeclaration owner,
    final RCTTypeDeclarationType type,
    final RCCName name)
  {
    return new ConsumerBuilder(owner, type, name);
  }

  public static ProducerBuilder producerBuilder(
    final RCTOperationDeclaration owner,
    final RCTTypeDeclarationType type,
    final RCCName name)
  {
    return new ProducerBuilder(owner, type, name);
  }

  public static ModifierBuilder modifierBuilder(
    final RCTOperationDeclaration owner,
    final RCTTypeDeclarationType type,
    final RCCName name)
  {
    return new ModifierBuilder(owner, type, name);
  }

  private static <A, B> SortedMap<A, B> sortedMapCopyOf(
    final SortedMap<A, B> m)
  {
    return Collections.unmodifiableSortedMap(new TreeMap<>(m));
  }

  private static <A, S> SortedMap<A, Set<S>> sortedMapCopyOfSet(
    final SortedMap<A, HashSet<S>> m)
  {
    return Collections.unmodifiableSortedMap(new TreeMap<>(m));
  }

  public static sealed abstract class AbstractBuilder
    permits ConsumerBuilder, ProducerBuilder, ModifierBuilder
  {
    private final RCTOperationDeclaration owner;
    private final RCTTypeDeclarationType type;
    private final RCCName name;
    private final RCTAccessSet.BuilderType accessBuilder;

    protected final RCTAccessSet.BuilderType accessBuilder()
    {
      return this.accessBuilder;
    }

    private AbstractBuilder(
      final RCTOperationDeclaration inOwner,
      final RCTTypeDeclarationType inType,
      final RCCName inName)
    {
      this.owner =
        Objects.requireNonNull(inOwner, "owner");
      this.type =
        Objects.requireNonNull(inType, "type");
      this.name =
        Objects.requireNonNull(inName, "inName");
      this.accessBuilder =
        RCTAccessSet.builder(inType);
    }

    public final RCCName name()
    {
      return this.name;
    }

    public final RCTOperationDeclaration owner()
    {
      return this.owner;
    }

    public final RCTTypeDeclarationType type()
    {
      return this.type;
    }

    public void addReadsAll(
      final RCGCommandPipelineStage stage)
    {
      this.accessBuilder.addReadsAll(stage);
    }

    public void addReadsSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      this.accessBuilder.addReadsSpecific(path, stage);
    }

    public void addWritesAll(
      final RCGCommandPipelineStage stage)
    {
      this.accessBuilder.addWritesAll(stage);
    }

    public void addWritesSpecific(
      final RCCPath path,
      final RCGCommandPipelineStage stage)
    {
      this.accessBuilder.addWritesSpecific(path, stage);
    }

    public void addEnsuresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      this.accessBuilder.setEnsuresImageLayoutSpecific(path, layout);
    }

    public void addEnsuresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      this.accessBuilder.setEnsuresImageLayoutForAllImages(layout);
    }

    public void addRequiresImageLayoutForAllImages(
      final RCGResourceImageLayout layout)
    {
      this.accessBuilder.setRequiresImageLayoutForAllImages(layout);
    }

    public void addRequiresImageLayoutSpecific(
      final RCCPath path,
      final RCGResourceImageLayout layout)
    {
      this.accessBuilder.setRequiresImageLayoutSpecific(path, layout);
    }
  }

  public static final class ConsumerBuilder
    extends AbstractBuilder
  {
    private ConsumerBuilder(
      final RCTOperationDeclaration inOwner,
      final RCTTypeDeclarationType inType,
      final RCCName inName)
    {
      super(inOwner, inType, inName);
    }

    public RCTPortConsumer build()
    {
      return new RCTPortConsumer(
        this.owner(),
        this.type(),
        this.name(),
        this.accessBuilder().build()
      );
    }
  }

  public static final class ProducerBuilder
    extends AbstractBuilder
  {
    private ProducerBuilder(
      final RCTOperationDeclaration inOwner,
      final RCTTypeDeclarationType inType,
      final RCCName inName)
    {
      super(inOwner, inType, inName);
    }

    public RCTPortProducer build()
    {
      return new RCTPortProducer(
        this.owner(),
        this.type(),
        this.name(),
        this.accessBuilder().build()
      );
    }
  }

  public static final class ModifierBuilder
    extends AbstractBuilder
  {
    private ModifierBuilder(
      final RCTOperationDeclaration inOwner,
      final RCTTypeDeclarationType inType,
      final RCCName inName)
    {
      super(inOwner, inType, inName);
    }

    public RCTPortModifier build()
    {
      return new RCTPortModifier(
        this.owner(),
        this.type(),
        this.name(),
        this.accessBuilder().build()
      );
    }
  }
}
