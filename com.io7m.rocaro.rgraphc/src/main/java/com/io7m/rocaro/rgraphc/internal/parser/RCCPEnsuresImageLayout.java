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
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutForAllImages;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutForImage;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutType;
import org.xml.sax.Attributes;

import java.util.Optional;

/**
 * A parser.
 */

public final class RCCPEnsuresImageLayout
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclEnsuresImageLayoutType>
{
  private RCUDeclEnsuresImageLayoutType result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPEnsuresImageLayout(
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
        .orElseGet(() -> new RCUDeclEnsuresImageLayoutForAllImages(layout));
  }

  private static RCUDeclEnsuresImageLayoutType specific(
    final RCCPath name,
    final RCGResourceImageLayout layout)
  {
    return new RCUDeclEnsuresImageLayoutForImage(layout, name);
  }

  @Override
  public RCUDeclEnsuresImageLayoutType onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
