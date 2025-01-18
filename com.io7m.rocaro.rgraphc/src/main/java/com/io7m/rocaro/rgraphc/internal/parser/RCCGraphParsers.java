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

import com.io7m.anethum.api.ParseStatus;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.rgraphc.internal.RCCGraphParserFactoryType;
import com.io7m.rocaro.rgraphc.internal.RCCGraphParserType;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

public final class RCCGraphParsers
  extends RCObject
  implements RCCGraphParserFactoryType
{
  public RCCGraphParsers()
  {

  }

  @Override
  public RCCGraphParserType createParserWithContext(
    final RCCGraphParserParameters context,
    final URI source,
    final InputStream stream,
    final Consumer<ParseStatus> statusConsumer)
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(statusConsumer, "statusConsumer");

    final var newContext =
      Objects.requireNonNullElse(
        context,
        new RCCGraphParserParameters(
          new JXEHardenedSAXParsers(),
          false
        )
      );

    return new RCCGraphParser(
      newContext,
      source,
      stream,
      statusConsumer
    );
  }
}
