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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortProducerType;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintBuffer;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintImage;
import com.io7m.rocaro.api.graph.RCGPortTypeConstraintType;

import java.util.Objects;
import java.util.Set;

/**
 * A producer port.
 *
 * @param owner          The owner
 * @param name           The name
 * @param readsOnStages  The stages at which the resource is read
 * @param typeProduced   The resource type constraint
 * @param writesOnStages The stages at which the resource is written
 */

public record RCGPortProducer(
  RCGOperationType owner,
  RCGPortName name,
  Set<RCGCommandPipelineStage> readsOnStages,
  RCGPortTypeConstraintType<?> typeProduced,
  Set<RCGCommandPipelineStage> writesOnStages)
  implements RCGPortProducerType
{
  /**
   * A producer port.
   *
   * @param owner          The owner
   * @param name           The name
   * @param readsOnStages  The stages at which the resource is read
   * @param typeProduced   The resource type constraint
   * @param writesOnStages The stages at which the resource is written
   */

  public RCGPortProducer
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(typeProduced, "typeProduced");

    readsOnStages =
      Set.copyOf(readsOnStages);
    writesOnStages =
      Set.copyOf(writesOnStages);

    switch (typeProduced) {
      case final RCGPortTypeConstraintBuffer<?> _ -> {
        // Nothing required.
      }
      case final RCGPortTypeConstraintImage<?> c -> {
        Preconditions.checkPreconditionV(
          c.requiresImageLayout().isPresent(),
          "Producer ports must require an image layout for resources."
        );
      }
    }
  }
}
