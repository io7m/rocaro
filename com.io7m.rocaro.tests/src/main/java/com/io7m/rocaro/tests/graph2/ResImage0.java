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


package com.io7m.rocaro.tests.graph2;

import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.images.RCImageID;
import com.io7m.rocaro.api.resources.RCResourceSchematicImage2DType;

import java.util.Objects;
import java.util.UUID;

final class ResImage0 implements RCImage2DType
{
  private final RCResourceSchematicImage2DType schematic;
  private final RCImageID id;

  public ResImage0(
    final RCResourceSchematicImage2DType inSchematic)
  {
    this.schematic =
      Objects.requireNonNull(inSchematic, "s");
    this.id =
      new RCImageID(UUID.randomUUID());
  }

  @Override
  public String toString()
  {
    return "[ResImage0 0x%s]"
      .formatted(Integer.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public RCResourceSchematicImage2DType schematic()
  {
    return this.schematic;
  }

  @Override
  public RCImageID id()
  {
    return this.id;
  }

  @Override
  public VulkanFormat format()
  {
    return VulkanFormat.VK_FORMAT_R8_UNORM;
  }

  @Override
  public VulkanImageType data()
  {
    throw new UnimplementedCodeException();
  }

  @Override
  public VulkanImageViewType view()
  {
    throw new UnimplementedCodeException();
  }
}
