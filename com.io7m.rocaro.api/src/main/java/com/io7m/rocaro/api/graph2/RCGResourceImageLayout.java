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

/**
 * An image layout.
 */

public enum RCGResourceImageLayout
{
  /**
   * An undefined image layout.
   */

  LAYOUT_UNDEFINED,

  /**
   * An image layout that is optimal for reading from shaders.
   */

  LAYOUT_OPTIMAL_FOR_SHADER_READ,

  /**
   * An image layout that is optimal for rendering to as an attachment.
   */

  LAYOUT_OPTIMAL_FOR_ATTACHMENT,

  /**
   * An image layout that is optimal for use as a source in transfer operations.
   */

  LAYOUT_OPTIMAL_FOR_TRANSFER_SOURCE,

  /**
   * An image layout that is optimal for use as a destination in transfer operations.
   */

  LAYOUT_OPTIMAL_FOR_TRANSFER_TARGET,

  /**
   * An image layout that is optimal for presentation to the screen.
   */

  LAYOUT_OPTIMAL_FOR_PRESENTATION
}
