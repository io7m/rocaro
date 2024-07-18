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

import java.util.Objects;
import java.util.Set;

import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_ABGR;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_ARGB;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_BGRA;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_R;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RG;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RGB;
import static com.io7m.rocaro.api.images.RCImageColorChannels.COLOR_CHANNELS_RGBA;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.BLITTING_SOURCE;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.BLITTING_TARGET;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.RENDERING;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.RENDERING_BLENDING;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.SAMPLING;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.SAMPLING_LINEAR_FILTER;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.STORAGE;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.STORAGE_ATOMIC;
import static com.io7m.rocaro.api.images.RCImageFormatCapability.STORAGE_TEXEL;

/**
 * <p>Color formats.</p>
 *
 * <p>Each color format provides a set of {@link RCImageFormatCapability}
 * values that indicate the capabilities required to be supported by the Vulkan
 * specification.</p>
 */

public enum RCImageColorFormat
  implements RCImageFormatType
{
  /**
   * A four-component, 16-bit packed unsigned normalized format that has a 1-bit A component in bit
   * 15, a 5-bit R component in bits 10..14, a 5-bit G component in bits 5..9, and a 5-bit B
   * component in bits 0..4.
   */

  COLOR_FORMAT_A1R5G5B5_UNSIGNED_NORMALIZED_PACK16(
    COLOR_CHANNELS_ARGB,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit packed unsigned integer format that has a 2-bit A component in bits
   * 30..31, a 10-bit B component in bits 20..29, a 10-bit G component in bits 10..19, and a 10-bit
   * R component in bits 0..9.
   */

  COLOR_FORMAT_A2B10G10R10_UNSIGNED_INTEGER_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A four-component, 32-bit packed unsigned normalized format that has a 2-bit A component in bits
   * 30..31, a 10-bit B component in bits 20..29, a 10-bit G component in bits 10..19, and a 10-bit
   * R component in bits 0..9.
   */

  COLOR_FORMAT_A2B10G10R10_UNSIGNED_NORMALIZED_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit packed signed integer format that has an 8-bit A component in bits
   * 24..31, an 8-bit B component in bits 16..23, an 8-bit G component in bits 8..15, and an 8-bit R
   * component in bits 0..7.
   */

  COLOR_FORMAT_A8B8G8R8_SIGNED_INTEGER_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 32-bit packed unsigned normalized format that has an 8-bit A component in
   * bits 24..31, an 8-bit B component stored with sRGB nonlinear encoding in bits 16..23, an 8-bit
   * G component stored with sRGB nonlinear encoding in bits 8..15, and an 8-bit R component stored
   * with sRGB nonlinear encoding in bits 0..7.
   */

  COLOR_FORMAT_A8B8G8R8_SRGB_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit packed unsigned integer format that has an 8-bit A component in bits
   * 24..31, an 8-bit B component in bits 16..23, an 8-bit G component in bits 8..15, and an 8-bit R
   * component in bits 0..7.
   */

  COLOR_FORMAT_A8B8G8R8_UNSIGNED_INTEGER_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 32-bit packed unsigned normalized format that has an 8-bit A component in
   * bits 24..31, an 8-bit B component in bits 16..23, an 8-bit G component in bits 8..15, and an
   * 8-bit R component in bits 0..7.
   */

  COLOR_FORMAT_A8B8G8R8_UNSIGNED_NORMALIZED_PACK32(
    COLOR_CHANNELS_ABGR,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 32-bit unsigned normalized format that has an 8-bit B component stored with
   * sRGB nonlinear encoding in byte 0, an 8-bit G component stored with sRGB nonlinear encoding in
   * byte 1, an 8-bit R component stored with sRGB nonlinear encoding in byte 2, and an 8-bit A
   * component in byte 3.
   */

  COLOR_FORMAT_B8G8R8A8_SRGB(
    COLOR_CHANNELS_BGRA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit unsigned normalized format that has an 8-bit B component in byte 0, an
   * 8-bit G component in byte 1, an 8-bit R component in byte 2, and an 8-bit A component in byte
   * 3.
   */

  COLOR_FORMAT_B8G8R8A8_UNSIGNED_NORMALIZED(
    COLOR_CHANNELS_BGRA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 64-bit signed floating-point format that has a 16-bit R component in bytes
   * 0..1, a 16-bit G component in bytes 2..3, a 16-bit B component in bytes 4..5, and a 16-bit A
   * component in bytes 6..7.
   */

  COLOR_FORMAT_R16G16B16A16_SIGNED_FLOAT(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 64-bit signed integer format that has a 16-bit R component in bytes 0..1, a
   * 16-bit G component in bytes 2..3, a 16-bit B component in bytes 4..5, and a 16-bit A component
   * in bytes 6..7.
   */

  COLOR_FORMAT_R16G16B16A16_SIGNED_INTEGER(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 64-bit signed integer format that has a 16-bit R component in bytes 0..1, a
   * 16-bit G component in bytes 2..3, a 16-bit B component in bytes 4..5, and a 16-bit A component
   * in bytes 6..7.
   */

  COLOR_FORMAT_R16G16B16A16_UNSIGNED_INTEGER(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A two-component, 32-bit signed floating-point format that has a 16-bit R component in bytes
   * 0..1, and a 16-bit G component in bytes 2..3.
   */

  COLOR_FORMAT_R16G16_SIGNED_FLOAT(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A two-component, 32-bit signed integer format that has a 16-bit R component in bytes 0..1, and
   * a 16-bit G component in bytes 2..3.
   */

  COLOR_FORMAT_R16G16_SIGNED_INTEGER(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A two-component, 32-bit unsigned integer format that has a 16-bit R component in bytes 0..1,
   * and a 16-bit G component in bytes 2..3.
   */

  COLOR_FORMAT_R16G16_UNSIGNED_INTEGER(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A one-component, 16-bit signed floating-point format that has a single 16-bit R component.
   */

  COLOR_FORMAT_R16_SIGNED_FLOAT(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A one-component, 16-bit signed integer format that has a single 16-bit R component.
   */

  COLOR_FORMAT_R16_SIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A one-component, 16-bit unsigned integer format that has a single 16-bit R component.
   */

  COLOR_FORMAT_R16_UNSIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A one-component, 32-bit signed integer format that has a single 32-bit R component.
   */

  COLOR_FORMAT_R32_SIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE_ATOMIC,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A one-component, 32-bit unsigned integer format that has a single 32-bit R component.
   */

  COLOR_FORMAT_R32_UNSIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE_ATOMIC,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A three-component, 16-bit packed unsigned normalized format that has a 5-bit R component in
   * bits 11..15, a 6-bit G component in bits 5..10, and a 5-bit B component in bits 0..4.
   */

  COLOR_FORMAT_R5G6B5_UNSIGNED_NORMALIZED_PACK16(
    COLOR_CHANNELS_RGB,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit signed integer format that has an 8-bit R component in byte 0, an
   * 8-bit G component in byte 1, an 8-bit B component in byte 2, and an 8-bit A component in byte
   * 3.
   */

  COLOR_FORMAT_R8G8B8A8_SIGNED_INTEGER(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 32-bit unsigned normalized format that has an 8-bit R component stored with
   * sRGB nonlinear encoding in byte 0, an 8-bit G component stored with sRGB nonlinear encoding in
   * byte 1, an 8-bit B component stored with sRGB nonlinear encoding in byte 2, and an 8-bit A
   * component in byte 3.
   */

  COLOR_FORMAT_R8G8B8A8_SRGB(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A four-component, 32-bit unsigned integer format that has an 8-bit R component in byte 0, an
   * 8-bit G component in byte 1, an 8-bit B component in byte 2, and an 8-bit A component in byte
   * 3.
   */

  COLOR_FORMAT_R8G8B8A8_UNSIGNED_INTEGER(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A four-component, 32-bit unsigned normalized format that has an 8-bit R component in byte 0, an
   * 8-bit G component in byte 1, an 8-bit B component in byte 2, and an 8-bit A component in byte
   * 3.
   */

  COLOR_FORMAT_R8G8B8A8_UNSIGNED_NORMALIZED(
    COLOR_CHANNELS_RGBA,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER,
      STORAGE,
      STORAGE_TEXEL
    )
  ),

  /**
   * A two-component, 16-bit signed integer format that has an 8-bit R component in byte 0, and an
   * 8-bit G component in byte 1.
   */

  COLOR_FORMAT_R8G8_SIGNED_INTEGER(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A two-component, 16-bit unsigned integer format that has an 8-bit R component in byte 0, and an
   * 8-bit G component in byte 1.
   */

  COLOR_FORMAT_R8G8_UNSIGNED_INTEGER(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A two-component, 16-bit unsigned normalized format that has an 8-bit R component in byte 0, and
   * an 8-bit G component in byte 1.
   */

  COLOR_FORMAT_R8G8_UNSIGNED_NORMALIZED(
    COLOR_CHANNELS_RG,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  ),

  /**
   * A one-component, 8-bit signed integer format that has a single 8-bit R component.
   */

  COLOR_FORMAT_R8_SIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A one-component, 8-bit unsigned integer format that has a single 8-bit R component.
   */

  COLOR_FORMAT_R8_UNSIGNED_INTEGER(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      SAMPLING
    )
  ),

  /**
   * A one-component, 8-bit unsigned normalized format that has a single 8-bit R component.
   */

  COLOR_FORMAT_R8_UNSIGNED_NORMALIZED(
    COLOR_CHANNELS_R,
    Set.of(
      BLITTING_TARGET,
      BLITTING_SOURCE,
      RENDERING,
      RENDERING_BLENDING,
      SAMPLING,
      SAMPLING_LINEAR_FILTER
    )
  );

  private final RCImageColorChannels channels;
  private final Set<RCImageFormatCapability> capabilities;

  RCImageColorFormat(
    final RCImageColorChannels inChannels,
    final Set<RCImageFormatCapability> inCapabilities)
  {
    this.channels =
      Objects.requireNonNull(inChannels, "channels");
    this.capabilities =
      Objects.requireNonNull(inCapabilities, "inCapabilities");
  }

  /**
   * @return The color channels in this format
   */

  public RCImageColorChannels channels()
  {
    return this.channels;
  }

  /**
   * @return The set of capabilities guaranteed by the Vulkan specification
   */

  public Set<RCImageFormatCapability> capabilities()
  {
    return this.capabilities;
  }
}
