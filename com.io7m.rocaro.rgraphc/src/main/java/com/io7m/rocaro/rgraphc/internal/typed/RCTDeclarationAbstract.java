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


package com.io7m.rocaro.rgraphc.internal.typed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.net.URI;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public abstract class RCTDeclarationAbstract
  implements RCTDeclarationMetaType
{
  private final RCTGraphDeclarationType owner;

  @JsonProperty(value = "Name")
  private final RCCName name;
  @JsonProperty(value = "Comment")
  private String comment = "";
  @JsonProperty(value = "Position")
  private LexicalPosition<URI> lexical = LexicalPositions.zero();

  public RCTDeclarationAbstract(
    final RCTGraphDeclarationType owner,
    final RCCName inName)
  {
    this.owner =
      Objects.requireNonNull(owner, "owner");
    this.name =
      Objects.requireNonNull(inName, "name");
  }

  @Override
  public final void setLexical(
    final LexicalPosition<URI> inPosition)
  {
    this.lexical = Objects.requireNonNull(inPosition, "position");
  }

  @Override
  public final LexicalPosition<URI> lexical()
  {
    return this.lexical;
  }

  @Override
  public final RCTGraphDeclarationType graph()
  {
    return this.owner;
  }

  @Override
  public final String comment()
  {
    return this.comment;
  }

  @Override
  public final RCCName name()
  {
    return this.name;
  }

  @Override
  public final void setComment(
    final String text)
  {
    this.comment = text.trim();
  }
}
