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


package com.io7m.rocaro.vanilla.internal.images;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanImageType;
import com.io7m.jcoronado.api.VulkanImageViewType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.images.RCImageColorBlendableType;

import java.util.Objects;

import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT;

/**
 * A blendable image.
 *
 * @param size   The size
 * @param data   The image data
 * @param view   The image view
 * @param format The image format
 */

public record RCImageBlendable(
  Vector2I size,
  VulkanImageType data,
  VulkanImageViewType view,
  VulkanFormat format)
  implements RCImageColorBlendableType
{
  /**
   * A blendable image.
   *
   * @param size   The size
   * @param data   The image data
   * @param view   The image view
   * @param format The image format
   */

  public RCImageBlendable
  {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(view, "view");
    Objects.requireNonNull(format, "format");

    final var features = format.mandatoryFeatures();
    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
      _ -> "Feature set must contain VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT"
    );
    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
      _ -> "Feature set must contain VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT"
    );
    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
      _ -> "Feature set must contain VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT"
    );
  }
}
