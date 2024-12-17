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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.rocaro.api.graph2.RCGOperationFramePresentType;
import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGPortConsumes;
import com.io7m.rocaro.api.graph2.RCGPortName;
import com.io7m.rocaro.api.graph2.RCGResourceFrameImageType;

import java.util.Optional;
import java.util.Set;

import static com.io7m.rocaro.api.graph2.RCGCommandPipelineStage.STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT;
import static com.io7m.rocaro.api.graph2.RCGResourceImageLayout.LAYOUT_OPTIMAL_FOR_PRESENTATION;

/**
 * The frame presentation operation.
 */

public final class RCGOperationFramePresent
  extends RCGOperationAbstract
  implements RCGOperationFramePresentType
{
  private final RCGPortConsumes frame;

  /**
   * The frame presentation operation.
   *
   * @param inName The name
   */

  public RCGOperationFramePresent(
    final RCGOperationName inName)
  {
    super(inName);

    this.frame =
      this.addPort(
        new RCGPortConsumes(
          this,
          new RCGPortName("Frame"),
          RCGResourceFrameImageType.class,
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Set.of(STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT),
          Optional.of(LAYOUT_OPTIMAL_FOR_PRESENTATION)
        )
      );
  }

  @Override
  public RCGPortConsumes frame()
  {
    return this.frame;
  }
}
