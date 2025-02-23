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

import com.io7m.lanark.core.RDottedName;

import java.util.Comparator;
import java.util.Objects;

/**
 * The unique identifier for an asset.
 *
 * @param packageName The asset package name
 * @param path        The asset path
 */

public record RCAssetIdentifier(
  RDottedName packageName,
  RCAssetPath path)
  implements Comparable<RCAssetIdentifier>
{
  /**
   * The unique identifier for an asset.
   *
   * @param packageName The asset package name
   * @param path        The asset path
   */

  public RCAssetIdentifier
  {
    Objects.requireNonNull(packageName, "packageName");
    Objects.requireNonNull(path, "path");
  }

  @Override
  public String toString()
  {
    return "%s:%s".formatted(
      this.packageName,
      this.path
    );
  }

  @Override
  public int compareTo(
    final RCAssetIdentifier other)
  {
    return Comparator.comparing(RCAssetIdentifier::packageName)
      .thenComparing(RCAssetIdentifier::path)
      .compare(this, other);
  }
}
