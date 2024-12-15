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

import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCStandardErrorCodes;
import com.io7m.rocaro.api.assets.RCAssetException;
import com.io7m.rocaro.api.assets.RCAssetLoaderDirectoryBuilderType;
import com.io7m.rocaro.api.assets.RCAssetLoaderDirectoryType;
import com.io7m.rocaro.api.assets.RCAssetLoaderFactoryType;
import com.io7m.rocaro.api.assets.RCAssetType;
import com.io7m.rocaro.vanilla.internal.RCStringConstants;
import com.io7m.rocaro.vanilla.internal.RCStrings;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The default asset loader directory.
 */

public final class RCAssetLoaderDirectory
  extends RCObject
  implements RCAssetLoaderDirectoryType
{
  private final RCStrings strings;
  private final Map<Class<?>, RCAssetLoaderFactoryType<?>> loaders;

  private RCAssetLoaderDirectory(
    final RCStrings inStrings,
    final Map<Class<?>, RCAssetLoaderFactoryType<?>> inLoaders)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.loaders =
      Objects.requireNonNull(inLoaders, "loaders");
  }

  /**
   * Create an asset loader directory builder.
   *
   * @param locale The locale
   *
   * @return The builder
   */

  public static RCAssetLoaderDirectoryBuilderType builder(
    final Locale locale)
  {
    return new Builder(new RCStrings(locale));
  }

  @Override
  public <A extends RCAssetType> RCAssetLoaderFactoryType<A>
  findLoaderForClass(
    final Class<A> clazz)
    throws RCAssetException
  {
    return (RCAssetLoaderFactoryType<A>)
      Optional.ofNullable(this.loaders.get(clazz))
        .orElseThrow(() -> {
          return this.errorNoSuchLoader(clazz);
        });
  }

  private RCAssetException errorNoSuchLoader(
    final Class<?> clazz)
  {
    return new RCAssetException(
      this.strings.format(
        RCStringConstants.ERROR_LOADER_NOT_AVAILABLE_FOR_CLASS
      ),
      Map.of(
        this.strings.format(RCStringConstants.CLASS),
        clazz.getName()
      ),
      RCStandardErrorCodes.NONEXISTENT_ASSET_LOADER.codeName(),
      Optional.empty()
    );
  }

  @Override
  public String description()
  {
    return "Asset loader directory service.";
  }

  private static final class Builder
    implements RCAssetLoaderDirectoryBuilderType
  {
    private final RCStrings strings;
    private final HashMap<Class<?>, RCAssetLoaderFactoryType<?>> loaders;

    private Builder(
      final RCStrings inStrings)
    {
      this.strings =
        Objects.requireNonNull(inStrings, "strings");
      this.loaders =
        new HashMap<>();
    }

    @Override
    public RCAssetLoaderDirectoryBuilderType addLoader(
      final RCAssetLoaderFactoryType<?> factory)
      throws RCAssetException
    {
      final var clazz = factory.assetClass();
      if (this.loaders.containsKey(clazz)) {
        throw this.errorClassAlreadyUsed(clazz);
      }
      this.loaders.put(clazz, factory);
      return this;
    }

    private RCAssetException errorClassAlreadyUsed(
      final Class<? extends RCAssetType> clazz)
    {
      return new RCAssetException(
        this.strings.format(
          RCStringConstants.ERROR_LOADER_ALREADY_REGISTERED_FOR_CLASS
        ),
        Map.of(
          this.strings.format(RCStringConstants.CLASS),
          clazz.getName()
        ),
        RCStandardErrorCodes.DUPLICATE_ASSET_LOADER.codeName(),
        Optional.empty()
      );
    }

    @Override
    public RCAssetLoaderDirectoryType build()
    {
      return new RCAssetLoaderDirectory(
        this.strings,
        Map.copyOf(this.loaders)
      );
    }
  }
}
