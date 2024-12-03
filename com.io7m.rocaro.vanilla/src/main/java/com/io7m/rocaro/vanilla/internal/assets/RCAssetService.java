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

import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCStandardErrorCodes;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.assets.RCAssetException;
import com.io7m.rocaro.api.assets.RCAssetIdentifier;
import com.io7m.rocaro.api.assets.RCAssetLoaderDirectoryType;
import com.io7m.rocaro.api.assets.RCAssetReferenceType;
import com.io7m.rocaro.api.assets.RCAssetResolutionContextType;
import com.io7m.rocaro.api.assets.RCAssetResolverType;
import com.io7m.rocaro.api.assets.RCAssetServiceType;
import com.io7m.rocaro.api.assets.RCAssetType;
import com.io7m.rocaro.api.assets.RCAssetValueFailed;
import com.io7m.rocaro.api.assets.RCAssetValueLoading;
import com.io7m.rocaro.api.assets.RCAssetValueType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ASSET;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_ASSET_DOES_NOT_EXIST;

public final class RCAssetService
  extends RCObject
  implements RCAssetServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCAssetService.class);

  private final AtomicBoolean closed;
  private final CloseableCollectionType<RocaroException> resources;
  private final Duration queueFrequency;
  private final ExecutorService ioExecutor;
  private final ExecutorService taskExecutor;
  private final LinkedBlockingQueue<RCAssetReference<?>> queue;
  private final RCAssetLoaderDirectoryType loaders;
  private final RCAssetResolverType resolver;
  private final RCStrings strings;
  private final FileSystem realFileSystem;
  private final FileSystem moduleFileSystem;

  private RCAssetService(
    final RCAssetLoaderDirectoryType loaders,
    final Duration queueFrequency,
    final RCStrings strings,
    final RCAssetResolverType inResolver)
  {
    this.loaders =
      Objects.requireNonNull(loaders, "loaders");
    this.queueFrequency =
      Objects.requireNonNull(queueFrequency, "queueFrequency");
    this.resolver =
      Objects.requireNonNull(inResolver, "resolver");
    this.strings =
      Objects.requireNonNull(strings, "strings");

    this.resources =
      RCResourceCollections.create(strings);
    this.closed =
      new AtomicBoolean(false);

    this.realFileSystem =
      FileSystems.getDefault();
    this.moduleFileSystem =
      FileSystems.getFileSystem(URI.create("jrt:/"));

    this.taskExecutor =
      this.resources.add(
        Executors.newThreadPerTaskExecutor(
          Thread.ofVirtual()
            .name("com.io7m.rocaro.asset.controller-", 0)
            .factory()
        )
      );

    this.ioExecutor =
      this.resources.add(
        Executors.newThreadPerTaskExecutor(
          Thread.ofVirtual()
            .name("com.io7m.rocaro.asset.io-", 0)
            .factory()
        )
      );

    this.queue =
      new LinkedBlockingQueue<>();
  }

  public static RCAssetService create(
    final RPServiceDirectoryType services,
    final Duration queueFrequency)
  {
    final var strings =
      services.requireService(RCStrings.class);
    final var resolver =
      services.requireService(RCAssetResolverType.class);
    final var loaders =
      services.requireService(RCAssetLoaderDirectoryType.class);

    final var service =
      new RCAssetService(
        loaders,
        queueFrequency,
        strings,
        resolver
      );

    service.start();
    return service;
  }

  private static void logAssetControllerCrash(
    final Throwable e)
  {
    final var ev = new RCAssetControllerFailed();
    if (ev.shouldCommit()) {
      ev.message = e.getMessage();
      ev.begin();
      ev.end();
      ev.commit();
    }
  }

  private static void logAssetLoadFailure(
    final RCAssetReference<?> ref,
    final Throwable e)
  {
    final var ev = new RCAssetLoadFailed();
    if (ev.shouldCommit()) {
      ev.packageName = ref.identifier.packageName().value();
      ev.path = ref.identifier.path().toString();
      ev.assetClass = ref.assetClass.getName();
      ev.message = e.getMessage();
      ev.begin();
      ev.end();
      ev.commit();
    }
  }

  private void start()
  {
    this.taskExecutor.execute(this::run);
  }

  private void run()
  {
    while (!this.closed.get()) {
      try {
        final var ref =
          this.queue.poll(this.queueFrequency.toNanos(), TimeUnit.NANOSECONDS);

        if (ref != null) {
          this.ioExecutor.execute(() -> this.processNewAsset(ref));
        }
      } catch (final Throwable e) {
        logAssetControllerCrash(e);
      }
    }
  }

  private void processNewAsset(
    final RCAssetReference<?> ref)
  {
    final var ev =
      RCAssetLoading.ofIdentifier(ref.identifier);
    ev.begin();

    try {
      try (final var processResources =
             RCResourceCollections.create(this.strings)) {

        final var loaderFactory =
          this.loaders.findLoaderForClass(ref.assetClass);
        final var loader =
          processResources.add(loaderFactory.createLoader());
        final var context =
          processResources.add(
            new AssetResolutionContext(
              this.strings,
              Arena.ofShared(),
              this.realFileSystem,
              this.moduleFileSystem
            )
          );

        final var resolvedOpt =
          this.resolver.resolve(context, ref.identifier);

        if (resolvedOpt.isEmpty()) {
          ref.setValue(
            new RCAssetValueFailed<>(this.errorAssetNonexistent(ref))
          );
          throw new NoSuchFileException(
            this.strings.format(ERROR_ASSET_DOES_NOT_EXIST)
          );
        }

        final var resolved =
          processResources.add(resolvedOpt.get());

      }
    } catch (final Throwable e) {
      logAssetLoadFailure(ref, e);
    } finally {
      ev.end();
      ev.commit();
    }
  }

  private RCAssetException errorAssetNonexistent(
    final RCAssetReference<?> ref)
  {
    return new RCAssetException(
      this.strings.format(ERROR_ASSET_DOES_NOT_EXIST),
      Map.ofEntries(
        Map.entry(
          this.strings.format(ASSET),
          ref.identifier.toString()
        )
      ),
      RCStandardErrorCodes.NONEXISTENT_ASSET.codeName(),
      Optional.empty()
    );
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
    this.closed.set(true);
    this.resources.close();
  }

  @Override
  public <A extends RCAssetType> RCAssetReferenceType<A> openAsset(
    final RCAssetIdentifier identifier,
    final Class<A> assetClass)
  {
    Objects.requireNonNull(identifier, "identifier");
    Objects.requireNonNull(assetClass, "assetClass");

    final var ref = new RCAssetReference<>(identifier, assetClass);
    this.queue.add(ref);
    return ref;
  }

  @Override
  public String description()
  {
    return "Asset service.";
  }

  private static final class AssetResolutionContext
    implements RCAssetResolutionContextType, RCCloseableType
  {
    private final Arena arena;
    private final FileSystem realFileSystem;
    private final FileSystem moduleFileSystem;
    private final CloseableCollectionType<RocaroException> resources;

    AssetResolutionContext(
      final RCStrings strings,
      final Arena inArena,
      final FileSystem inRealFileSystem,
      final FileSystem inModuleFileSystem)
    {
      this.resources =
        RCResourceCollections.create(strings);
      this.arena =
        this.resources.add(inArena);
      this.realFileSystem =
        this.resources.add(inRealFileSystem);
      this.moduleFileSystem =
        this.resources.add(inModuleFileSystem);
    }

    @Override
    public Arena arena()
    {
      return this.arena;
    }

    @Override
    public FileSystem realFileSystem()
    {
      return this.realFileSystem;
    }

    @Override
    public FileSystem moduleFileSystem()
    {
      return this.moduleFileSystem;
    }

    @Override
    public void close()
      throws RocaroException
    {
      this.resources.close();
    }
  }

  private static final class RCAssetReference<A extends RCAssetType>
    implements RCAssetReferenceType<A>
  {
    private final RCAssetIdentifier identifier;
    private final Class<A> assetClass;
    private AtomicReference<RCAssetValueType<A>> value;

    RCAssetReference(
      final RCAssetIdentifier inIdentifier,
      final Class<A> inAssetClass)
    {
      this.identifier =
        Objects.requireNonNull(inIdentifier, "identifier");
      this.assetClass =
        Objects.requireNonNull(inAssetClass, "assetClass");
      this.value =
        new AtomicReference<>(
          new RCAssetValueLoading<>(0.0, Optional.empty())
        );
    }

    @Override
    public RCAssetIdentifier identifier()
    {
      return this.identifier;
    }

    @Override
    public RCAssetValueType<A> get()
    {
      return this.value.get();
    }

    @Override
    public void close()
    {

    }

    public void setValue(
      final RCAssetValueType<A> value)
    {
      this.value.set(Objects.requireNonNull(value, "value"));
    }
  }
}
