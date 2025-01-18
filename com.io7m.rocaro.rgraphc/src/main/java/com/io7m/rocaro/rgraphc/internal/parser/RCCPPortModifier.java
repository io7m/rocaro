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
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclEnsuresImageLayoutType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclPortModifier;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRequiresImageLayoutType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryAccessReadsType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUMemoryAccessWritesType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUTypeReference;
import org.xml.sax.Attributes;

import java.util.Map;

import static com.io7m.rocaro.rgraphc.internal.parser.RC1.element;

/**
 * A parser.
 */

public final class RCCPPortModifier
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclPortModifier>
{
  private RCUDeclPortModifier result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPPortModifier(
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
        element("TypeReference"),
        RCCPTypeReference::new
      ),
      Map.entry(
        element("Reads"),
        RCCPReads::new
      ),
      Map.entry(
        element("Writes"),
        RCCPWrites::new
      ),
      Map.entry(
        element("RequiresImageLayout"),
        RCCPRequiresImageLayout::new
      ),
      Map.entry(
        element("EnsuresImageLayout"),
        RCCPEnsuresImageLayout::new
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
      case final RCUTypeReference r -> {
        this.result.setType(r);
      }
      case final RCUMemoryAccessReadsType r -> {
        this.result.addReadsAt(r);
      }
      case final RCUMemoryAccessWritesType w -> {
        this.result.addWritesAt(w);
      }
      case final RCUDeclEnsuresImageLayoutType e -> {
        this.result.addEnsuresImageLayout(e);
      }
      case final RCUDeclRequiresImageLayoutType r -> {
        this.result.addRequiresImageLayout(r);
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
      new RCUDeclPortModifier(
        new RCCName(
          attributes.getValue("Name")
        )
      );

    this.result.setLexical(
      RCCPLexical.fromDocument(context.documentLocator())
    );
  }

  @Override
  public RCUDeclPortModifier onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
