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


package com.io7m.rocaro.api.images;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcoronado.api.VulkanFormat;

import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_BLIT_DST_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_BLIT_SRC_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT;
import static com.io7m.jcoronado.api.VulkanFormatFeatureFlag.VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT;

/**
 * The preconditions for image formats.
 */

public final class RCImageFormatPreconditions
{
  private RCImageFormatPreconditions()
  {

  }

  /**
   * Check the given format against the preconditions required to classify it
   * as a basic color image.
   *
   * @param format The format
   */

  public static void checkColorBasicPreconditions(
    final VulkanFormat format)
  {
    final var features = format.mandatoryFeatures();

    /*
     * Basic requirements.
     *
     * VK_FORMAT_FEATURE_TRANSFER_DST_BIT and
     * VK_FORMAT_FEATURE_TRANSFER_SRC_BIT are implied by
     * VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT and may not be explicitly exposed.
     */

    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_BLIT_SRC_BIT),
      _ -> {
        return "Feature set for format %s contain VK_FORMAT_FEATURE_BLIT_SRC_BIT"
          .formatted(format);
      }
    );
    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_BLIT_DST_BIT),
      _ -> {
        return "Feature set for format %s contain VK_FORMAT_FEATURE_BLIT_DST_BIT"
          .formatted(format);
      }
    );
    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
      _ -> {
        return "Feature set for format %s contain VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT"
          .formatted(format);
      }
    );
  }

  /**
   * Check the given format against the preconditions required to classify it
   * as a renderable color image.
   *
   * @param format The format
   */

  public static void checkColorRenderablePreconditions(
    final VulkanFormat format)
  {
    checkColorBasicPreconditions(format);

    final var features = format.mandatoryFeatures();

    /*
     * Renderable requirements.
     */

    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
      _ -> {
        return "Feature set for format %s contain VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT"
          .formatted(format);
      }
    );
  }

  /**
   * Check the given format against the preconditions required to classify it
   * as a blendable color image.
   *
   * @param format The format
   */

  public static void checkColorBlendablePreconditions(
    final VulkanFormat format)
  {
    checkColorBasicPreconditions(format);
    checkColorRenderablePreconditions(format);

    final var features = format.mandatoryFeatures();

    /*
     * Blendable requirements.
     */

    Preconditions.checkPrecondition(
      features,
      f -> f.contains(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
      _ -> {
        return "Feature set for format %s contain VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT"
          .formatted(format);
      }
    );
  }
}
