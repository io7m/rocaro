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
public final class RCTDepthAttachment
  implements RCTAttachmentType
{
  private final RCTTypeDeclarationRenderTarget owner;

  @JsonProperty("Name")
  private final RCCName name;

  public RCTDepthAttachment(
    final RCTTypeDeclarationRenderTarget inOwner,
    final RCCName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "inName");
    this.owner =
      Objects.requireNonNull(inOwner, "owner");
  }

  @Override
  public String toString()
  {
    return "[RCTDepthAttachment %s]".formatted(this.name);
  }

  @Override
  public String kind()
  {
    return "DepthAttachment";
  }

  @Override
  public RCCName name()
  {
    return this.name;
  }

  @Override
  public RCTTypeDeclarationRenderTarget owner()
  {
    return this.owner;
  }
}
