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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.io7m.rocaro.api.graph.RCGSubmissionID;
import com.io7m.rocaro.rgraphc.internal.RCCPath;

import java.io.IOException;

public final class RCGSubmissionIDSerializer
  extends JsonSerializer<RCGSubmissionID>
{
  public RCGSubmissionIDSerializer()
  {

  }

  @Override
  public Class<RCGSubmissionID> handledType()
  {
    return RCGSubmissionID.class;
  }

  @Override
  public void serialize(
    final RCGSubmissionID value,
    final JsonGenerator gen,
    final SerializerProvider serializers)
    throws IOException
  {
    gen.writeStartObject();
    gen.writeFieldName("ID");
    gen.writeNumber(value.submissionId());
    gen.writeFieldName("Queue");
    gen.writeString(value.queue().name());
    gen.writeEndObject();
  }
}
