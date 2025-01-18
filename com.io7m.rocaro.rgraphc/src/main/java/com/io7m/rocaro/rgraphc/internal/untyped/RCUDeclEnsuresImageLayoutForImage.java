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


package com.io7m.rocaro.rgraphc.internal.untyped;

import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.RCCPath;

import java.util.Objects;

public final class RCUDeclEnsuresImageLayoutForImage
  extends RCUGraphElement
  implements RCUDeclEnsuresImageLayoutType
{
  private final RCCPath image;
  private final RCGResourceImageLayout layout;

  public RCUDeclEnsuresImageLayoutForImage(
    final RCGResourceImageLayout inLayout,
    final RCCPath inImage)
  {
    this.layout =
      Objects.requireNonNull(inLayout, "layout");
    this.image =
      Objects.requireNonNull(inImage, "image");
  }

  public RCCPath image()
  {
    return this.image;
  }

  @Override
  public RCGResourceImageLayout layout()
  {
    return this.layout;
  }
}
