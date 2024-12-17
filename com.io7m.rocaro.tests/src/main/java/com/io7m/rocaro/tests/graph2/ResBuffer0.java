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


package com.io7m.rocaro.tests.graph2;

import com.io7m.rocaro.api.graph2.RCGNoParameters;
import com.io7m.rocaro.api.graph2.RCGResourceBufferType;
import com.io7m.rocaro.api.graph2.RCGResourceFactoryType;
import com.io7m.rocaro.api.graph2.RCGResourceName;

import java.util.Objects;

final class ResBuffer0
  implements RCGResourceBufferType
{
  private final RCGResourceName name;

  public ResBuffer0(
    final RCGResourceName inName)
  {
    this.name = Objects.requireNonNull(inName, "name");
  }

  public static RCGResourceFactoryType<RCGNoParameters, ResBuffer0> factory()
  {
    return (name, _) -> new ResBuffer0(name);
  }

  @Override
  public RCGResourceName name()
  {
    return this.name;
  }
}
