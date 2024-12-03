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


package com.io7m.rocaro.vanilla;

import com.io7m.lanark.core.RDottedName;
import com.io7m.rocaro.api.assets.RCAssetResolverBuilderType;
import com.io7m.rocaro.api.assets.RCAssetResolverType;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.assets.RCAssetResolverComposite;
import com.io7m.rocaro.vanilla.internal.assets.RCAssetResolverFS;
import com.io7m.rocaro.vanilla.internal.assets.RCAssetResolverModulePath;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RCAssetResolvers
{
  private RCAssetResolvers()
  {

  }

  public static RCAssetResolverBuilderType builder(
    final Locale locale)
  {
    return new Builder(new RCStrings(locale));
  }

  private static final class Builder
    implements RCAssetResolverBuilderType
  {
    private final ArrayList<RCAssetResolverType> resolvers;
    private final RCStrings strings;

    Builder(
      final RCStrings inStrings)
    {
      this.strings =
        Objects.requireNonNull(inStrings, "strings");
      this.resolvers =
        new ArrayList<>();
    }

    @Override
    public RCAssetResolverBuilderType addBaseDirectory(
      final Path directory)
    {
      Objects.requireNonNull(directory, "directory");

      this.resolvers.add(new RCAssetResolverFS(this.strings, directory));
      return this;
    }

    @Override
    public RCAssetResolverBuilderType addPackage(
      final Module module,
      final RDottedName packageName)
    {
      this.resolvers.add(
        new RCAssetResolverModulePath(this.strings, module, packageName)
      );
      return this;
    }

    @Override
    public RCAssetResolverType build()
    {
      return new RCAssetResolverComposite(
        List.copyOf(this.resolvers)
      );
    }
  }
}
