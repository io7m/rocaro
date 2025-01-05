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

package com.io7m.rocaro.api.services;

import com.io7m.repetoir.core.RPServiceType;
import com.io7m.rocaro.api.RCRendererID;

/**
 * <p>
 * A service scoped to the current renderer.
 * </p>
 * <p>
 * That is, the service's lifetime is equal to that of the renderer. Any request
 * made to retrieve an instance of the service anywhere in the renderer will
 * result in the same service instance being obtained.
 * </p>
 */

public interface RCServiceRendererScopedType
  extends RPServiceType
{
  /**
   * @return The ID of the renderer to which this service instance belongs
   */

  RCRendererID rendererId();
}
