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
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPLexical;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclConnect;
import org.xml.sax.Attributes;

/**
 * A parser.
 */

public final class RCCPConnect
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclConnect>
{
  private RCUDeclConnect result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPConnect(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.result =
      new RCUDeclConnect(
        new RCCName(attributes.getValue("SourceOperation")),
        new RCCName(attributes.getValue("SourcePort")),
        new RCCName(attributes.getValue("TargetOperation")),
        new RCCName(attributes.getValue("TargetPort"))
      );

    this.result.setLexical(
      RCCPLexical.fromDocument(context.documentLocator())
    );
  }

  @Override
  public RCUDeclConnect onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
