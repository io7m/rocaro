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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;

import java.util.Objects;
import java.util.Set;

/**
 * An access command.
 */

public final class RCCAccess
  extends RCCAbstractCommand
  implements RCCSyncCommandType
{
  private final RCCExecute owner;
  private final RCCPPlaceholderType resource;
  private final Set<RCGCommandPipelineStage> readsAt;
  private final RCCPortPrimitiveType port;
  private final Set<RCGCommandPipelineStage> writesAt;

  /**
   * An access command.
   *
   * @param id         The ID
   * @param inOwner    The executing command
   * @param inResource The resource
   * @param inWritesAt The stages at which writes occur
   * @param inReadsAt  The stages at which reads occur
   * @param inPort     The port
   */

  public RCCAccess(
    final long id,
    final RCCExecute inOwner,
    final RCCPPlaceholderType inResource,
    final Set<RCGCommandPipelineStage> inWritesAt,
    final Set<RCGCommandPipelineStage> inReadsAt,
    final RCCPortPrimitiveType inPort)
  {
    super(id);

    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.resource =
      Objects.requireNonNull(inResource, "resource");

    this.writesAt =
      Set.copyOf(inWritesAt);
    this.readsAt =
      Set.copyOf(inReadsAt);
    this.port =
      Objects.requireNonNull(inPort, "port");

    Preconditions.checkPreconditionV(
      (!this.writesAt.isEmpty()) || (!this.readsAt.isEmpty()),
      "At least one read or write stage must be used."
    );
  }

  /**
   * @return The port to which the access belongs
   */

  public RCCPortPrimitiveType port()
  {
    return this.port;
  }

  /**
   * @return The owner of the access
   */

  public RCCExecute owner()
  {
    return this.owner;
  }

  /**
   * @return The resource
   */

  public RCCPPlaceholderType resource()
  {
    return this.resource;
  }

  /**
   * @return The stages at which the read accesses occur
   */

  public Set<RCGCommandPipelineStage> readsAt()
  {
    return this.readsAt;
  }

  /**
   * @return The stages at which the write accesses occur
   */

  public Set<RCGCommandPipelineStage> writesAt()
  {
    return this.writesAt;
  }

  @Override
  public RCTOperationDeclaration operation()
  {
    return this.owner.operation();
  }
}
