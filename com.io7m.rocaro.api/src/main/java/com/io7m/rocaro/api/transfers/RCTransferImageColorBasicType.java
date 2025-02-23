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


package com.io7m.rocaro.api.transfers;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jcoronado.api.VulkanFormat;
import com.io7m.jcoronado.api.VulkanImageLayout;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.images.RCImageColorBasicType;
import com.io7m.rocaro.api.images.RCImageFormatPreconditions;
import org.immutables.value.Value;

import java.util.UUID;

/**
 * The parameters required to transfer a single basic color image to the GPU.
 */

@Value.Immutable
@ImmutablesStyleType
public non-sealed interface RCTransferImageColorBasicType
  extends RCTransferOperationType<RCImageColorBasicType>
{
  @Override
  @Value.Default
  default UUID id()
  {
    return UUID.randomUUID();
  }

  /**
   * @return A humanly-readable name for the image, for debugging
   */

  String name();

  /**
   * @return The size of the image
   */

  Vector2I size();

  /**
   * @return The format of the image
   */

  VulkanFormat format();

  /**
   * @return The final image layout
   */

  VulkanImageLayout finalLayout();

  /**
   * @return A copying function to populate the image with data
   */

  RCTransferCopyFunctionType dataCopier();

  /**
   * @return The queue that will own the image when the operation is completed
   */

  RCDeviceQueueCategory targetQueue();

  /**
   * Check preconditions for the image.
   */

  @Value.Check
  default void checkPreconditions()
  {
    RCImageFormatPreconditions.checkColorBasicPreconditions(this.format());
  }
}
