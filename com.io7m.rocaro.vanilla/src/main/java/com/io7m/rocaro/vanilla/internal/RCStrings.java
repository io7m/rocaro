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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.jxtrand.api.JXTStringConstantType;
import com.io7m.jxtrand.vanilla.JXTAbstractStrings;
import com.io7m.repetoir.core.RPServiceType;

import java.util.Locale;

/**
 * The string resources.
 */

public final class RCStrings
  extends JXTAbstractStrings
  implements RPServiceType
{
  /**
   * The string resources.
   *
   * @param locale The locale
   */

  public RCStrings(
    final Locale locale)
  {
    super(
      locale,
      RCStrings.class,
      "/com/io7m/rocaro/vanilla/internal/",
      "Strings"
    );
  }

  @Override
  public String format(
    final JXTStringConstantType id,
    final Object... args)
  {
    return super.format(id, args).trim();
  }

  @Override
  public String format(
    final String id,
    final Object... args)
  {
    return super.format(id, args).trim();
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Long.toUnsignedString(this.hashCode(), 16)
    );
  }

  @Override
  public String description()
  {
    return "String service.";
  }
}
