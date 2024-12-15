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

import java.util.Optional;
import java.util.Set;

/**
 * The type of ports that are consumers.
 */

public sealed interface RCGPortConsumerType
  extends RCGPortType
  permits RCGPortConsumes, RCGPortModifies
{
  /**
   * @return The image layout requirement, if any
   */

  Optional<RCGResourceImageLayout> requiresImageLayout();

  /**
   * A set of stages at which the operation that owns this port will read
   * from the resource connected to the port.
   *
   * @return The read stages
   */

  Set<RCGCommandPipelineStage> readsOnStages();

  /**
   * A set of stages at which the operation that owns this port will write
   * to the resource connected to the port.
   *
   * @return The write stages
   */

  Set<RCGCommandPipelineStage> writesOnStages();
}
