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
 * Functions over resource types.
 */

public final class RCGResourceTypes
{
  private RCGResourceTypes()
  {

  }

  /**
   * @param clazz The class
   *
   * @return {@code true} if the given class is an image class
   */

  public static boolean isImage(
    final Class<?> clazz)
  {
    return RCGResourceImageType.class.isAssignableFrom(clazz);
  }

  /**
   * Determine if the given target port can accept values from the given
   * source port.
   *
   * @param source The source port
   * @param target The target port
   *
   * @return {@code true} if the ports are type-compatible.
   */

  public static boolean targetCanAcceptSource(
    final RCGPortProducerType source,
    final RCGPortConsumerType target)
  {
    return target.type().isAssignableFrom(source.type());
  }
}
