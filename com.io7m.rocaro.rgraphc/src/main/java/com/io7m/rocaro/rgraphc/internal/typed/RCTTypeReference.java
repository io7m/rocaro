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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.jlexing.core.LexicalType;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCPackageName;

import java.net.URI;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public final class RCTTypeReference
  implements LexicalType<URI>, RCTLexicalType
{
  private final RCCPackageName packageName;
  private final RCCName name;
  private final RCTTypeDeclarationType type;
  private LexicalPosition<URI> position = LexicalPositions.zero();

  public RCTTypeReference(
    final RCCPackageName inPackageName,
    final RCCName inName,
    final RCTTypeDeclarationType inType)
  {
    this.packageName =
      Objects.requireNonNull(inPackageName, "packageName");
    this.name =
      Objects.requireNonNull(inName, "name");
    this.type =
      Objects.requireNonNull(inType, "type");
  }

  public void setLexical(
    final LexicalPosition<URI> inPosition)
  {
    this.position = Objects.requireNonNull(inPosition, "position");
  }

  public RCCPackageName packageName()
  {
    return this.packageName;
  }

  public RCCName name()
  {
    return this.name;
  }

  public RCTTypeDeclarationType type()
  {
    return this.type;
  }

  @Override
  public boolean equals(
    final Object obj)
  {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final var that = (RCTTypeReference) obj;
    return Objects.equals(this.packageName, that.packageName)
           && Objects.equals(this.name, that.name)
           && Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.packageName, this.name, this.type);
  }

  @Override
  public String toString()
  {
    return "[RCTTypeReference %s %s %s]"
      .formatted(
        this.packageName,
        this.name,
        this.type
      );
  }

  @Override
  public LexicalPosition<URI> lexical()
  {
    return this.position;
  }
}
