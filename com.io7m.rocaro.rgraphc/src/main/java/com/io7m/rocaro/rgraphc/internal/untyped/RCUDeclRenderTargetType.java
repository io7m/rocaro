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


package com.io7m.rocaro.rgraphc.internal.untyped;

import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RCUDeclRenderTargetType
  extends RCUGraphElement
  implements RCUTypeDeclarationType
{
  private final RCCName name;
  private final ArrayList<RCUDeclColorAttachment> colorAttachments;
  private final List<RCUDeclColorAttachment> colorAttachmentsRead;
  private Optional<RCUDeclDepthAttachment> depthAttachment;

  public RCUDeclRenderTargetType(
    final RCCName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.colorAttachments =
      new ArrayList<>();
    this.colorAttachmentsRead =
      Collections.unmodifiableList(this.colorAttachments);
    this.depthAttachment =
      Optional.empty();
  }

  @Override
  public RCCName name()
  {
    return this.name;
  }

  public void addColorAttachment(
    final RCUDeclColorAttachment c)
  {
    this.colorAttachments.add(c);
  }

  public void setDepthAttachment(
    final RCUDeclDepthAttachment c)
  {
    if (this.depthAttachment.isPresent()) {
      throw new IllegalArgumentException(
        "A depth attachment is already assigned."
      );
    }
    this.depthAttachment = Optional.of(c);
  }

  public List<RCUDeclColorAttachment> colorAttachments()
  {
    return this.colorAttachmentsRead;
  }

  public Optional<RCUDeclDepthAttachment> depthAttachment()
  {
    return this.depthAttachment;
  }
}
