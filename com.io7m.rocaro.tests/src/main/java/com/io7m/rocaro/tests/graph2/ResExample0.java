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

import com.io7m.rocaro.api.graph.RCGNoParameters;
import com.io7m.rocaro.api.graph.RCGResourceFactoryType;
import com.io7m.rocaro.api.graph.RCGResourceName;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;

import java.util.Objects;

final class ResExample0
  implements RCGResourcePlaceholderType
{
  private final RCGResourceName name;

  public ResExample0(
    final RCGResourceName inName)
  {
    this.name = Objects.requireNonNull(inName, "name");
  }

  public static RCGResourceFactoryType<RCGNoParameters, ResExample0> factory()
  {
    return (name, _) -> new ResExample0(name);
  }

  @Override
  public String toString()
  {
    return "[ResExample0 %s]".formatted(this.name);
  }

  @Override
  public RCGResourceName name()
  {
    return this.name;
  }
}
