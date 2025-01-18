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


package com.io7m.rocaro.rgraphc.internal;

import com.io7m.jlexing.core.LexicalPosition;
import org.xml.sax.ext.Locator2;

import java.net.URI;
import java.util.Optional;

public final class RCCPLexical
{
  private RCCPLexical()
  {

  }

  public static LexicalPosition<URI> fromDocument(
    final Locator2 locator2)
  {
    final var file =
      Optional.ofNullable(locator2.getSystemId())
        .map(URI::create);

    return LexicalPosition.<URI>builder()
      .setFile(file)
      .setColumn(locator2.getColumnNumber())
      .setLine(locator2.getLineNumber())
      .build();
  }

  public static String showPosition(
    final LexicalPosition<URI> position)
  {
    return position.file()
      .map(x -> "%s:%d:%d".formatted(x, position.line(), position.column()))
      .orElseGet(() -> "%d:%d".formatted(position.line(), position.column()));
  }
}
