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


package com.io7m.rocaro.api.graph2;

import com.io7m.jaffirm.core.Preconditions;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A port that produces an output.
 *
 * @param owner              The owning operation
 * @param name               The name of the port
 * @param type               The type of the port values
 * @param readsOnStages      The stages at which the resource will be read
 * @param writesOnStages     The stages at which the resource will be written
 * @param ensuresImageLayout The ensured image layout, for image-typed resources
 */

public record RCGPortProduces(
  RCGOperationType owner,
  RCGPortName name,
  Class<? extends RCGResourceType> type,
  Set<RCGCommandPipelineStage> readsOnStages,
  Set<RCGCommandPipelineStage> writesOnStages,
  Optional<RCGResourceImageLayout> ensuresImageLayout)
  implements RCGPortProducerType
{
  /**
   * A port that produces an output.
   *
   * @param owner              The owning operation
   * @param name               The name of the port
   * @param type               The type of the port values
   * @param readsOnStages      The stages at which the resource will be read
   * @param writesOnStages     The stages at which the resource will be written
   * @param ensuresImageLayout The ensured image layout, for image-typed resources
   */

  public RCGPortProduces
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(ensuresImageLayout, "ensuresImageLayout");

    readsOnStages = Set.copyOf(readsOnStages);
    writesOnStages = Set.copyOf(writesOnStages);

    if (RCGResourceTypes.isImage(type)) {
      Preconditions.checkPreconditionV(
        ensuresImageLayout.isPresent(),
        "Pure producer ports must provide image layout assurances."
      );
    }

    if (ensuresImageLayout.isPresent()) {
      Preconditions.checkPreconditionV(
        type,
        RCGResourceTypes.isImage(type),
        "Ports can only specify image layout assurances for image resources."
      );
    }
  }
}
