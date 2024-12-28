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


package com.io7m.rocaro.vanilla.internal.assets;

import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.assets.RCAssetException;
import com.io7m.rocaro.api.assets.RCAssetIdentifier;
import com.io7m.rocaro.api.assets.RCAssetResolutionContextType;
import com.io7m.rocaro.api.assets.RCAssetResolvedType;
import com.io7m.rocaro.api.assets.RCAssetResolverType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A composite asset resolver that inspects many delegate resolvers.
 */

public final class RCAssetResolverComposite
  extends RCObject
  implements RCAssetResolverType
{
  private final List<RCAssetResolverType> resolvers;

  /**
   * A composite asset resolver that inspects many delegate resolvers.
   *
   * @param inResolvers The resolvers
   */

  public RCAssetResolverComposite(
    final List<RCAssetResolverType> inResolvers)
  {
    this.resolvers = List.copyOf(inResolvers);
  }

  @Override
  public Optional<RCAssetResolvedType> resolve(
    final RCAssetResolutionContextType context,
    final RCAssetIdentifier identifier)
    throws RCAssetException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(identifier, "identifier");

    for (final var resolver : this.resolvers) {
      final var r =
        resolver.resolve(context, identifier);

      if (r.isPresent()) {
        return r;
      }
    }

    return Optional.empty();
  }

  @Override
  public String description()
  {
    return "Composite asset resolver service.";
  }
}
