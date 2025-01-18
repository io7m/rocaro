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
import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
public final class RCTColorAttachment
  implements RCTAttachmentType
{
  @JsonProperty("Index")
  private final int index;
  private final RCTTypeDeclarationRenderTarget owner;
  @JsonProperty("Type")
  private final RCCName name;

  public RCTColorAttachment(
    final RCTTypeDeclarationRenderTarget inOwner,
    final RCCName inName,
    final int index)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.owner =
      Objects.requireNonNull(inOwner, "owner");
    this.index =
      index;
  }

  @Override
  public String toString()
  {
    return "[RCTColorAttachment %s %s]"
      .formatted(
        this.name,
        this.index
      );
  }

  @Override
  public RCTTypeDeclarationRenderTarget owner()
  {
    return this.owner;
  }

  @Override
  public RCCName name()
  {
    return this.name;
  }

  @Override
  public String kind()
  {
    return "ColorAttachment";
  }

  public int index()
  {
    return this.index;
  }
}
