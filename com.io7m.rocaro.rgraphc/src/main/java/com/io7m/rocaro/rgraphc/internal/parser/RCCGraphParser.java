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

import com.io7m.anethum.api.ParseSeverity;
import com.io7m.anethum.api.ParseStatus;
import com.io7m.anethum.api.ParsingException;
import com.io7m.blackthorne.core.BTElementHandlerConstructorType;
import com.io7m.blackthorne.core.BTException;
import com.io7m.blackthorne.core.BTParseError;
import com.io7m.blackthorne.core.BTPreserveLexical;
import com.io7m.blackthorne.core.BTQualifiedName;
import com.io7m.blackthorne.core.Blackthorne;
import com.io7m.blackthorne.jxe.BlackthorneJXE;
import com.io7m.jxe.core.JXEXInclude;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.rgraphc.internal.RCCGraphParserType;
import com.io7m.rocaro.rgraphc.internal.RCCSchemas;
import com.io7m.rocaro.rgraphc.internal.untyped.RCUDeclGraph;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.io7m.rocaro.rgraphc.internal.parser.RC1.element;

public final class RCCGraphParser
  extends RCObject
  implements RCCGraphParserType
{
  private final RCCGraphParserParameters context;
  private final URI source;
  private final InputStream stream;
  private final Consumer<ParseStatus> statusConsumer;

  /**
   * Package declaration parser.
   *
   * @param inContext        The SAX parsers
   * @param inSource         The source
   * @param inStatusConsumer The status consume
   * @param inStream         The input stream
   */

  public RCCGraphParser(
    final RCCGraphParserParameters inContext,
    final URI inSource,
    final InputStream inStream,
    final Consumer<ParseStatus> inStatusConsumer)
  {
    this.context =
      Objects.requireNonNull(inContext, "context");
    this.source =
      Objects.requireNonNull(inSource, "source");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.statusConsumer =
      Objects.requireNonNull(inStatusConsumer, "statusConsumer");
  }

  @Override
  public RCUDeclGraph execute()
    throws ParsingException
  {
    try {
      final RCUDeclGraph r;

      final Map<BTQualifiedName, BTElementHandlerConstructorType<?, RCUDeclGraph>> elements =
        Map.ofEntries(
          Map.entry(
            element("RenderGraph"),
            RCCPGraph::new
          )
        );

      if (this.context.skipXSDValidation()) {
        r = Blackthorne.parse(
          this.source,
          this.stream,
          BTPreserveLexical.PRESERVE_LEXICAL_INFORMATION,
          () -> {
            return this.context.parsers()
              .createXMLReaderNonValidating(
                Optional.empty(),
                JXEXInclude.XINCLUDE_DISABLED
              );
          },
          elements
        );
      } else {
        r = BlackthorneJXE.parseAll(
          this.source,
          this.stream,
          elements,
          this.context.parsers(),
          Optional.empty(),
          JXEXInclude.XINCLUDE_DISABLED,
          BTPreserveLexical.PRESERVE_LEXICAL_INFORMATION,
          RCCSchemas.schemas()
        );
      }

      return r;
    } catch (final BTException e) {
      final var statuses =
        e.errors()
          .stream()
          .map(RCCGraphParser::mapParseError)
          .toList();

      for (final var status : statuses) {
        this.statusConsumer.accept(status);
      }

      throw new ParsingException(e.getMessage(), List.copyOf(statuses));
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }

  private static ParseStatus mapParseError(
    final BTParseError error)
  {
    return ParseStatus.builder("parse-error", error.message())
      .withSeverity(mapSeverity(error.severity()))
      .withLexical(error.lexical())
      .build();
  }

  private static ParseSeverity mapSeverity(
    final BTParseError.Severity severity)
  {
    return switch (severity) {
      case ERROR -> ParseSeverity.PARSE_ERROR;
      case WARNING -> ParseSeverity.PARSE_WARNING;
    };
  }
}
