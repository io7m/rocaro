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


package com.io7m.rocaro.rgraphc.internal.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public final class RCCJson
{
  private RCCJson()
  {

  }

  public static ObjectMapper createObjectMapper()
  {
    final var module = new SimpleModule();
    module.addSerializer(new RCLexicalPositionSerializer());
    module.addSerializer(new RCCPortPathSerializer());
    module.addSerializer(new RCCPathSerializer());
    module.addSerializer(new RCGSubmissionIDSerializer());

    final var mapper = new ObjectMapper();
    mapper.registerModule(module);
    final var jdk8 = new Jdk8Module();
    mapper.registerModule(jdk8);
    return mapper;
  }
}
