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


package com.io7m.rocaro.api.images;

/**
 * The capabilities of a texture format.
 */

public enum RCImageFormatCapability
{
  /**
   * The format can be used in storage images.
   */

  STORAGE,

  /**
   * The format can be used in storage texel buffers.
   */

  STORAGE_TEXEL,

  /**
   * The format can be used for atomic operations in storage images.
   */

  STORAGE_ATOMIC,

  /**
   * The format can be used as a framebuffer color attachment and as an input
   * attachment.
   */

  RENDERING,

  /**
   * The format can be used as a framebuffer color attachment and as an input
   * attachment, and supports blending operations.
   */

  RENDERING_BLENDING,

  /**
   * The format can be used as a source for blitting operations.
   */

  BLITTING_SOURCE,

  /**
   * The format can be used as a target for blitting operations.
   */

  BLITTING_TARGET,

  /**
   * The format can be sampled from in shaders.
   */

  SAMPLING,

  /**
   * The format can be sampled from in shaders with bilinear filtering.
   */

  SAMPLING_LINEAR_FILTER
}
