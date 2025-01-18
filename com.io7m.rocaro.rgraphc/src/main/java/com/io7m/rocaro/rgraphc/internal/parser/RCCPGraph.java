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
import com.io7m.rocaro.rgraphc.internal.RCCPackageName;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclBufferType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclConnect;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclGraph;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclImageType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclOperation;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRecordType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclRenderTargetType;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUImport;
import org.xml.sax.Attributes;

import java.util.Map;

import static com.io7m.rocaro.rgraphc.internal.parser.RC1.element;

/**
 * A parser.
 */

public final class RCCPGraph
  extends RCObject
  implements BTElementHandlerType<Object, RCUDeclGraph>
{
  private RCUDeclGraph result;

  /**
   * A parser.
   *
   * @param context The context
   */

  public RCCPGraph(
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
        element("DeclareBufferType"),
        RCCPDeclareBufferType::new
      ),
      Map.entry(
        element("DeclareImageType"),
        RCCPDeclareImageType::new
      ),
      Map.entry(
        element("DeclareRenderTargetType"),
        RCCPDeclareRenderTargetType::new
      ),
      Map.entry(
        element("DeclareRecordType"),
        RCCPDeclareRecordType::new
      ),
      Map.entry(
        element("DeclareOperation"),
        RCCPOperation::new
      ),
      Map.entry(
        element("Connect"),
        RCCPConnect::new
      ),
      Map.entry(
        element("Import"),
        RCCPImport::new
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
    throws Exception
  {
    switch (result) {
      case final RCUDeclBufferType b -> {
        this.result.addElement(b);
      }
      case final RCUDeclImageType b -> {
        this.result.addElement(b);
      }
      case final RCUDeclRenderTargetType b -> {
        this.result.addElement(b);
      }
      case final RCUDeclRecordType b -> {
        this.result.addElement(b);
      }
      case final RCUDeclOperation b -> {
        this.result.addElement(b);
      }
      case final RCUDeclConnect b -> {
        this.result.addElement(b);
      }
      case final RCUImport b -> {
        this.result.addElement(b);
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
      new RCUDeclGraph(
        new RCCPackageName(
          attributes.getValue("Package")
        )
      );
  }

  @Override
  public RCUDeclGraph onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
