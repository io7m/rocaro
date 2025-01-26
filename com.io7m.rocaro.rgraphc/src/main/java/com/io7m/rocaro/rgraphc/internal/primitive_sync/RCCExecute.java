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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderImageType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderMemoryType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveProducer;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A pseudo-command representing the execution of an operation. Note that
 * this is essentially a readability aid for large graphs; the actual effects
 * of the operation are represented by the associated access commands.
 */

public final class RCCExecute
  extends RCCAbstractCommand
  implements RCCMetaCommandType
{
  private final SortedSet<RCCIntroduceType> intros;
  private final SortedSet<RCCAccess> accesses;
  private final RCTOperationDeclaration operation;
  private final SortedSet<RCCIntroduceType> introsR;
  private final SortedSet<RCCAccess> accessesR;

  RCCExecute(
    final long id,
    final RCTOperationDeclaration inOperation)
  {
    super(id);

    this.operation =
      Objects.requireNonNull(inOperation, "operation");
    this.intros =
      new TreeSet<>(RCCCommandType.idComparator());
    this.accesses =
      new TreeSet<>(RCCCommandType.idComparator());
    this.accessesR =
      Collections.unmodifiableSortedSet(this.accesses);
    this.introsR =
      Collections.unmodifiableSortedSet(this.intros);
  }

  @Override
  public RCTOperationDeclaration operation()
  {
    return this.operation;
  }

  /**
   * @return The read commands associated with the execution
   */

  public SortedSet<RCCAccess> accesses()
  {
    return this.accessesR;
  }

  /**
   * @return The intro commands associated with the execution
   */

  public SortedSet<RCCIntroduceType> introductions()
  {
    return this.introsR;
  }

  /**
   * Add an access command.
   *
   * @param id          The ID
   * @param resource    The resource
   * @param writeStages The write stages
   * @param readStages  The read stages
   * @param port        The primitive port
   *
   * @return The command
   */

  public RCCAccess addAccess(
    final long id,
    final RCCPPlaceholderType resource,
    final Set<RCGCommandPipelineStage> writeStages,
    final Set<RCGCommandPipelineStage> readStages,
    final RCCPortPrimitiveType port)
  {
    final var r =
      new RCCAccess(id, this, resource, writeStages, readStages, port);
    r.setComment("Access on port %s".formatted(port.fullPath()));
    this.accesses.add(r);
    return r;
  }

  /**
   * Add a memory intro command.
   *
   * @param id       The ID
   * @param resource The resource
   * @param port     The primitive port
   *
   * @return The command
   */

  public RCCIntroduceMemory addIntroMemory(
    final long id,
    final RCCPPlaceholderMemoryType resource,
    final RCCPortPrimitiveProducer port)
  {
    final var r = new RCCIntroduceMemory(id, this, resource, port);
    r.setComment("Intro on port %s".formatted(port.fullPath()));
    this.intros.add(r);
    return r;
  }

  /**
   * Add an image intro command.
   *
   * @param id          The ID
   * @param resource    The resource
   * @param port        The primitive port
   * @param imageLayout The image layout
   *
   * @return The command
   */

  public RCCIntroduceImage addIntroImage(
    final long id,
    final RCCPPlaceholderImageType resource,
    final RCCPortPrimitiveProducer port,
    final RCGResourceImageLayout imageLayout)
  {
    final var r = new RCCIntroduceImage(id, this, resource, port, imageLayout);
    r.setComment("Intro on port %s".formatted(port.fullPath()));
    this.intros.add(r);
    return r;
  }
}
