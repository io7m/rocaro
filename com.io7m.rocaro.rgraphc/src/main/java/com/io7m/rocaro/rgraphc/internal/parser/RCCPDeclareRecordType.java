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
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPLexical;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUComment;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclField;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRecordType;
import org.xml.sax.Attributes;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static com.io7m.rocaro.rgraphc.internal.parser.RC1.element;

/**
 * A parser.
 */

public final class RCCPDeclareRecordType
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclRecordType>
{
  private final ArrayList<RCUDeclField> fields;
  private RCCName name;
  private LexicalPosition<URI> position = LexicalPositions.zero();
  private String comment = "";

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPDeclareRecordType(
    final BTElementParsingContextType context)
  {
    this.fields = new ArrayList<>();
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
        element("Field"),
        RCCPField::new
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
        this.comment = c.text();
      }
      case final RCUDeclField f -> {
        this.fields.add(f);
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
    this.name =
      new RCCName(attributes.getValue("Name"));
    this.position =
      RCCPLexical.fromDocument(context.documentLocator());
  }

  @Override
  public RCUDeclRecordType onElementFinished(
    final BTElementParsingContextType context)
  {
    final var r = new RCUDeclRecordType(this.name, this.fields);
    r.setComment(this.comment);
    r.setLexical(this.position);
    return r;
  }
}
