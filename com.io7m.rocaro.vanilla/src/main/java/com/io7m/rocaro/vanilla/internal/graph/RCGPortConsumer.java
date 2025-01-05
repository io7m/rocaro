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

import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortConsumerType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintType;

import java.util.Objects;
import java.util.Set;

/**
 * A consumer port.
 *
 * @param owner          The owner
 * @param name           The name
 * @param readsOnStages  The stages at which the resource is read
 * @param typeConsumed   The resource type constraint
 * @param writesOnStages The stages at which the resource is written
 * @param <S>            The resource schematic type
 * @param <R>            The resource type
 */

public record RCGPortConsumer<
  R extends RCResourceType,
  S extends RCResourceSchematicType>(
  RCGOperationType owner,
  RCGPortName name,
  Set<RCGCommandPipelineStage> readsOnStages,
  RCSchematicConstraintType<S> typeConsumed,
  Set<RCGCommandPipelineStage> writesOnStages)
  implements RCGPortConsumerType<R>
{
  /**
   * A consumer port.
   *
   * @param owner          The owner
   * @param name           The name
   * @param readsOnStages  The stages at which the resource is read
   * @param typeConsumed   The resource type constraint
   * @param writesOnStages The stages at which the resource is written
   */

  public RCGPortConsumer
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(typeConsumed, "typeConsumed");

    readsOnStages =
      Set.copyOf(readsOnStages);
    writesOnStages =
      Set.copyOf(writesOnStages);
  }
}
