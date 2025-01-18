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


package com.io7m.rocaro.rgraphc.internal.parser;

import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.rgraphc.internal.RCCPath;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutForAllImages;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutForImage;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutType;
import org.xml.sax.Attributes;

import java.util.Optional;

/**
 * A parser.
 */

public final class RCCPRequiresImageLayout
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclRequiresImageLayoutType>
{
  private RCUDeclRequiresImageLayoutType result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPRequiresImageLayout(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    final var layout =
      RCGResourceImageLayout.valueOf(attributes.getValue("Layout"));

    final var nameOpt =
      Optional.ofNullable(attributes.getValue("Image"))
        .map(RCCPath::parse);

    this.result =
      nameOpt.map(name -> specific(name, layout))
        .orElseGet(() -> new RCUDeclRequiresImageLayoutForAllImages(layout));
  }

  private static RCUDeclRequiresImageLayoutType specific(
    final RCCPath name,
    final RCGResourceImageLayout layout)
  {
    return new RCUDeclRequiresImageLayoutForImage(layout, name);
  }

  @Override
  public RCUDeclRequiresImageLayoutType onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
