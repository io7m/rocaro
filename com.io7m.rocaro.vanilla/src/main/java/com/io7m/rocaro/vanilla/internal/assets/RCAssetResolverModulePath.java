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

import com.io7m.lanark.core.RDottedName;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCStandardErrorCodes;
import com.io7m.rocaro.api.assets.RCAssetException;
import com.io7m.rocaro.api.assets.RCAssetIdentifier;
import com.io7m.rocaro.api.assets.RCAssetResolutionContextType;
import com.io7m.rocaro.api.assets.RCAssetResolvedType;
import com.io7m.rocaro.api.assets.RCAssetResolverType;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCStringConstants;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An asset resolver that inspects a package on the module path.
 */

public final class RCAssetResolverModulePath
  extends RCObject
  implements RCAssetResolverType
{
  private final RCStrings strings;
  private final Module module;
  private final RDottedName packageName;

  /**
   * An asset resolver that inspects a package on the module path.
   *
   * @param inStrings     The string resources
   * @param inModule      The module
   * @param inPackageName The package name
   */

  public RCAssetResolverModulePath(
    final RCStrings inStrings,
    final Module inModule,
    final RDottedName inPackageName)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.module =
      Objects.requireNonNull(inModule, "module");
    this.packageName =
      Objects.requireNonNull(inPackageName, "packageName");
  }

  @Override
  public Optional<RCAssetResolvedType> resolve(
    final RCAssetResolutionContextType context,
    final RCAssetIdentifier identifier)
    throws RCAssetException
  {
    if (!Objects.equals(this.packageName, identifier.packageName())) {
      return Optional.empty();
    }

    final var packageBase =
      String.join("/", identifier.packageName()
        .segments());

    final var fullPath =
      "/%s%s".formatted(packageBase, identifier.path());

    final var moduleBase =
      context.moduleFileSystem()
        .getPath(this.module.getName());

    final var filePath =
      moduleBase.resolve(fullPath);

    final FileChannel channel;
    try {
      channel = FileChannel.open(filePath, StandardOpenOption.READ);
    } catch (final NoSuchFileException e) {
      return Optional.empty();
    } catch (final IOException e) {
      throw this.errorIO(e, identifier, filePath);
    }

    final MemorySegment map;
    try {
      map = channel.map(
        FileChannel.MapMode.READ_ONLY,
        0L,
        channel.size(),
        context.arena()
      );
    } catch (final IOException e) {
      throw this.errorIO(e, identifier, filePath);
    }

    return Optional.of(
      new RCAssetResolvedFileChannel(this.strings, channel, map)
    );
  }

  private RCAssetException errorIO(
    final IOException e,
    final RCAssetIdentifier identifier,
    final Path filePath)
  {
    return new RCAssetException(
      Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()),
      e,
      Map.ofEntries(
        Map.entry(
          this.strings.format(RCStringConstants.ASSET),
          identifier.toString()
        ),
        Map.entry(
          this.strings.format(RCStringConstants.FILE),
          filePath.toString()
        )
      ),
      RCStandardErrorCodes.IO.codeName(),
      Optional.empty()
    );
  }

  @Override
  public String description()
  {
    return "Module path asset resolver service.";
  }
}
