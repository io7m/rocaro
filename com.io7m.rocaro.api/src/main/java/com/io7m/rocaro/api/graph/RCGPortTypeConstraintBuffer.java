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


package com.io7m.rocaro.api.graph;

import java.util.Objects;

/**
 * A constraint that ensures a resource is a buffer.
 *
 * @param resourceType The precise resource type
 * @param <R>          The type of resource
 */

public record RCGPortTypeConstraintBuffer<
  R extends RCGResourcePlaceholderBufferType>(
  Class<R> resourceType)
  implements RCGPortTypeConstraintType<R>
{
  /**
   * A constraint that ensures a resource is a buffer.
   *
   * @param resourceType The precise resource type
   */

  public RCGPortTypeConstraintBuffer
  {
    Objects.requireNonNull(resourceType, "resourceType");
  }
}
