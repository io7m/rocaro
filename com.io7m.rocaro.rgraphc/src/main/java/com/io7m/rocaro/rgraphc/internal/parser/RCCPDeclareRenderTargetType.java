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

import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTElementHandlerType;
import com.io7m.blackthorne.core.BTElementParsingContextType;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPLexical;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUComment;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclColorAttachment;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclDepthAttachment;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRenderTargetType;
import org.xml.sax.Attributes;

import java.util.Map;

import static com.io7m.rocaro.rgraphc.internal.parser.RC1.element;

/**
 * A parser.
 */

public final class RCCPDeclareRenderTargetType
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclRenderTargetType>
{
  private RCUDeclRenderTargetType result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPDeclareRenderTargetType(
    final BTElementParsingContextType context)
  {

  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Comment"),
        RCCPComment::comment
      ),
      Map.entry(
        element("ColorAttachment"),
        RCCPColorAttachment::new
      ),
      Map.entry(
        element("DepthAttachment"),
        RCCPDepthAttachment::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    switch (result) {
      case final RCUComment c -> {
        this.result.setComment(c.text());
      }
      case final RCUDeclColorAttachment c -> {
        this.result.addColorAttachment(c);
      }
      case final RCUDeclDepthAttachment c -> {
        this.result.setDepthAttachment(c);
      }
      default -> {
        throw new IllegalStateException("Unexpected value: " + result);
      }
    }
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
  {
    this.result =
      new RCUDeclRenderTargetType(
        new RCCName(
          attributes.getValue("Name")
        )
      );

    this.result.setLexical(
      RCCPLexical.fromDocument(context.documentLocator())
    );
  }

  @Override
  public RCUDeclRenderTargetType onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
