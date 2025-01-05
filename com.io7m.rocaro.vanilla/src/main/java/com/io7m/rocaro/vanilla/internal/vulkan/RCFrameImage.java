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


package com.io7m.rocaro.vanilla.internal.vulkan;

import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.images.RCImageID;

import java.util.Objects;

/**
 * A frame image.
 *
 * @param id        The image ID
 * @param data      The image data
 * @param view      The image view
 * @param schematic The schematic
 */

public record RCFrameImage(
  RCImageID id,
  VulkanImageType data,
  VulkanImageViewType view,
  RCFrameImageSchematic schematic)
  implements RCImage2DType
{
  /**
   * A frame image.
   *
   * @param id        The image ID
   * @param data      The image data
   * @param view      The image view
   * @param schematic The schematic
   */

  public RCFrameImage
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(schematic, "schematic");
  }

  @Override
  public VulkanFormat format()
  {
    return this.schematic.format();
  }
}
