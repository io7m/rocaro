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


package com.io7m.rocaro.api.assets;

import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.devices.RCDeviceType;

/**
 * An asset loader.
 *
 * @param <P> The asset parameters
 * @param <A> The asset type
 */

public interface RCAssetLoaderType<
  P extends RCAssetParametersType,
  A extends RCAssetType>
  extends RCCloseableType
{
  /**
   * Load an asset.
   *
   * @param device     The device
   * @param parameters The parameters
   * @param asset      The asset
   *
   * @return The loaded asset
   */

  RCAssetValueType<A> load(
    RCDeviceType device,
    P parameters,
    RCAssetResolvedType asset
  );
}
