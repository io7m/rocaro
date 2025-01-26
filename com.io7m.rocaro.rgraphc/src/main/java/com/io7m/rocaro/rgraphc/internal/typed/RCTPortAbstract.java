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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.access_set.RCTAccessSetType;

import java.net.URI;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public abstract class RCTPortAbstract
  implements RCTPortMetaType, RCTLexicalType
{
  private final RCTOperationDeclaration owner;
  @JsonProperty("Type")
  @JsonSerialize(using = RCTTypeNameSerializer.class)
  private final RCTTypeDeclarationType type;
  @JsonProperty("Name")
  private final RCCName name;
  @JsonProperty("AccessSet")
  private final RCTAccessSetType accessSet;
  @JsonProperty("Position")
  private LexicalPosition<URI> lexical = LexicalPositions.zero();
  protected RCTPortAbstract(
    final RCTOperationDeclaration inOwner,
    final RCTTypeDeclarationType inType,
    final RCCName inName,
    final RCTAccessSetType inAccessSet)
  {
    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.type =
      Objects.requireNonNull(inType, "type");
    this.name =
      Objects.requireNonNull(inName, "name");
    this.accessSet =
      Objects.requireNonNull(inAccessSet, "accessSet");
  }

  @Override
  public final RCTAccessSetType accessSet()
  {
    return this.accessSet;
  }

  @Override
  public String toString()
  {
    return "[%s %s %s %s]".formatted(
      this.getClass().getSimpleName(),
      this.type,
      this.name,
      this.lexical
    );
  }

  @Override
  public final RCCName name()
  {
    return this.name;
  }

  @Override
  public final void setLexical(
    final LexicalPosition<URI> position)
  {
    this.lexical = Objects.requireNonNull(position, "position");
  }

  @Override
  public final LexicalPosition<URI> lexical()
  {
    return this.lexical;
  }

  @Override
  public final RCTOperationDeclaration owner()
  {
    return this.owner;
  }

  @Override
  public final RCTTypeDeclarationType type()
  {
    return this.type;
  }
}
