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


package com.io7m.rocaro.vanilla.internal.renderdoc;

import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RocaroException;

/**
 * The RenderDoc service.
 */

public final class RCRenderDocService
{
  private RCRenderDocService()
  {

  }

  /**
   * Create a RenderDoc service. A real RenderDoc implementation is used
   * if available, and a no-op implementation otherwise.
   *
   * @param rendererId The ID of the renderer that owns the service
   *
   * @return The service
   */

  public static RCRenderDocServiceType create(
    final RCRendererID rendererId)
  {
    try {
      return RCRenderDocFFM.create(rendererId);
    } catch (final RocaroException e) {
      return createNoOp(rendererId);
    }
  }

  /**
   * Create a no-op RenderDoc service.
   *
   * @param rendererId The ID of the renderer that owns the service
   *
   * @return The service
   */

  public static RCRenderDocServiceType createNoOp(
    final RCRendererID rendererId)
  {
    return RCRenderDocNoOp.create(rendererId);
  }
}
