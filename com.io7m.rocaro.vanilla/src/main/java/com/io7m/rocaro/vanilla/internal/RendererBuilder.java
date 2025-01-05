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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.jcoronado.lwjgl.VMALWJGLAllocatorProvider;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.rocaro.api.RCCloseableGPUType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RendererBuilderType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.assets.RCAssetResolverType;
import com.io7m.rocaro.api.assets.RCAssetServiceType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenPrimary;
import com.io7m.rocaro.api.displays.RCDisplaySelectionType;
import com.io7m.rocaro.api.graph.RCGGraphBuilderType;
import com.io7m.rocaro.api.graph.RCGGraphException;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.api.transfers.RCTransferServiceType;
import com.io7m.rocaro.vanilla.RCAssetResolvers;
import com.io7m.rocaro.vanilla.RCGraph;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.assets.RCAssetService;
import com.io7m.rocaro.vanilla.internal.frames.RCFrameService;
import com.io7m.rocaro.vanilla.internal.frames.RCFrameServiceType;
import com.io7m.rocaro.vanilla.internal.notifications.RCNotificationService;
import com.io7m.rocaro.vanilla.internal.notifications.RCNotificationServiceType;
import com.io7m.rocaro.vanilla.internal.renderdoc.RCRenderDocService;
import com.io7m.rocaro.vanilla.internal.renderdoc.RCRenderDocServiceType;
import com.io7m.rocaro.vanilla.internal.threading.RCStandardExecutors;
import com.io7m.rocaro.vanilla.internal.transfers.RCTransferService;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRenderer;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_GRAPH;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NAME_ALREADY_USED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;

/**
 * A renderer builder.
 */

public final class RendererBuilder
  implements RendererBuilderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RendererBuilder.class);

  private static final AtomicLong INSTANCE_IDS =
    new AtomicLong(0);

  private final TreeMap<RCGraphName, RCGGraphBuilderType> graphs;
  private final RCStrings strings;
  private final RCVersions versions;
  private final RCGLFWFacadeType glfw;
  private RendererVulkanConfiguration vulkanConfiguration;
  private RCDisplaySelectionType displaySelection;
  private RCAssetResolverType resolver;

  /**
   * A renderer builder.
   *
   * @param locale     The locale
   * @param inStrings  The string resources
   * @param inVersions The version resources
   * @param inGLFW     The GLFW facade
   */

  public RendererBuilder(
    final Locale locale,
    final RCStrings inStrings,
    final RCVersions inVersions,
    final RCGLFWFacadeType inGLFW)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.versions =
      Objects.requireNonNull(inVersions, "rcVersions");
    this.glfw =
      Objects.requireNonNull(inGLFW, "glfw");

    this.graphs =
      new TreeMap<>();

    this.vulkanConfiguration =
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(VulkanLWJGLInstanceProvider.create())
        .setVmaAllocators(VMALWJGLAllocatorProvider.create())
        .build();

    this.displaySelection =
      new RCDisplaySelectionFullscreenPrimary("Rocaro");

    this.resolver =
      RCAssetResolvers.builder(locale)
        .build();
  }

  @Override
  public RCGGraphBuilderType declareRenderGraph(
    final RCGraphName name)
    throws RCGGraphException
  {
    Objects.requireNonNull(name, "name");

    if (this.graphs.containsKey(name)) {
      throw this.errorGraphAlreadyExists(name);
    }

    final var builder = RCGraph.builder(this.strings, name);
    this.graphs.put(name, builder);
    return builder;
  }

  @Override
  public RendererBuilderType setAssetResolver(
    final RCAssetResolverType inResolver)
  {
    this.resolver = Objects.requireNonNull(inResolver, "resolver");
    return this;
  }

  @Override
  public RendererBuilderType setDisplaySelection(
    final RCDisplaySelectionType selection)
  {
    this.displaySelection =
      Objects.requireNonNull(selection, "selection");
    return this;
  }

  @Override
  public RendererBuilderType setVulkanConfiguration(
    final RendererVulkanConfiguration configuration)
  {
    this.vulkanConfiguration =
      Objects.requireNonNull(configuration, "vulkanConfiguration");
    return this;
  }

  @Override
  public RendererType start()
    throws RocaroException
  {
    final var id =
      new RCRendererID(freshID());
    final var executors =
      RCStandardExecutors.create(this.strings, id);

    try {
      return executors.mainExecutor().executeAndWait(() -> {
        LOG.debug("Starting renderer.");
        return this.createRenderer(id, executors);
      });
    } catch (final Exception e) {
      executors.close();
      throw e;
    }
  }

  private RendererType createRenderer(
    final RCRendererID rendererId,
    final RCStandardExecutors executors)
    throws Exception
  {
    final var exceptions =
      new ExceptionTracker<RocaroException>();
    final var services =
      new RPServiceDirectory();
    final var resources =
      RCResourceCollections.create(this.strings);

    try {
      createService(
        exceptions,
        services,
        executors,
        resources,
        RCStrings.class,
        () -> this.strings
      );

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCAssetResolverType.class,
        () -> this.resolver
      );

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCFrameServiceType.class,
        () -> RCFrameService.create(rendererId)
      );

      if (this.vulkanConfiguration.enableRenderDocSupport()) {
        createService(
          exceptions,
          services,
          executors,
          resources,
          RCRenderDocServiceType.class,
          () -> RCRenderDocService.create(rendererId)
        );
      } else {
        createService(
          exceptions,
          services,
          executors,
          resources,
          RCRenderDocServiceType.class,
          () -> RCRenderDocService.createNoOp(rendererId)
        );
      }

      final var builtGraphs =
        this.buildGraphs(exceptions);

      final var featuresRequired =
        builtGraphs.values()
          .stream()
          .map(RCGGraphType::requiredDeviceFeatures)
          .reduce(
            VulkanPhysicalDeviceFeaturesFunctions.none(),
            VulkanPhysicalDeviceFeaturesFunctions::or
          );

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCVulkanRendererType.class,
        () -> {
          return this.buildVulkanRenderer(
            services,
            executors,
            featuresRequired,
            rendererId
          );
        }
      );

      /*
       * Bail out early if the Vulkan renderer has failed.
       */

      exceptions.throwIfNecessary();

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCNotificationServiceType.class,
        () -> {
          return RCNotificationService.create(
            services,
            rendererId,
            this.vulkanConfiguration.notificationFrequency()
          );
        }
      );

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCTransferServiceType.class,
        () -> RCTransferService.create(services, rendererId)
      );

      createService(
        exceptions,
        services,
        executors,
        resources,
        RCAssetServiceType.class,
        () -> RCAssetService.create(services, rendererId)
      );

      exceptions.throwIfNecessary();

      LOG.debug("Created renderer.");
      return Renderer.create(
        services,
        services.requireService(RCStrings.class),
        executors,
        services.requireService(RCVulkanRendererType.class),
        services.requireService(RCFrameServiceType.class),
        resources,
        builtGraphs,
        rendererId
      );
    } catch (final Throwable e) {
      resources.close();
      throw e;
    }
  }

  private interface ServiceConstructorType<T>
  {
    T execute()
      throws RocaroException;
  }

  private static <T extends RPServiceType> void createService(
    final ExceptionTracker<RocaroException> tracker,
    final RPServiceDirectory services,
    final RCStandardExecutors executors,
    final CloseableCollectionType<RocaroException> resources,
    final Class<T> clazz,
    final ServiceConstructorType<T> callable)
  {
    try {
      LOG.debug("Creating service {}", clazz);
      final var service = callable.execute();
      if (service instanceof final RCCloseableType closeable) {
        if (closeable instanceof final RCCloseableGPUType gpuCloseable) {
          resources.add(() -> {
            executors.gpuExecutor().executeAndWait(() -> {
              gpuCloseable.close();
              return RCUnit.UNIT;
            });
          });
        } else {
          resources.add(closeable);
        }
      }
      services.register(clazz, service);
    } catch (final RocaroException e) {
      tracker.addException(e);
    }
  }

  private static long freshID()
  {
    return INSTANCE_IDS.incrementAndGet();
  }

  private RCVulkanRendererType buildVulkanRenderer(
    final RPServiceDirectory services,
    final RCStandardExecutors executors,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures,
    final RCRendererID rendererId)
    throws RocaroException
  {
    final var renderer =
      RCVulkanRenderer.create(
        services,
        executors,
        this.versions,
        this.glfw,
        this.vulkanConfiguration,
        this.displaySelection,
        requiredDeviceFeatures,
        rendererId
      );

    services.register(RCVulkanRendererType.class, renderer);
    return renderer;
  }

  private TreeMap<RCGraphName, RCGGraphType> buildGraphs(
    final ExceptionTracker<RocaroException> tracker)
  {
    final var builtGraphs = new TreeMap<RCGraphName, RCGGraphType>();
    for (final var graph : this.graphs.values()) {
      try {
        builtGraphs.put(graph.name(), graph.compile());
      } catch (final RCGGraphException e) {
        tracker.addException(e);
      }
    }
    return builtGraphs;
  }

  private RCGGraphException errorGraphAlreadyExists(
    final RCGraphName name)
  {
    return new RCGGraphException(
      this.strings.format(ERROR_GRAPH_NAME_ALREADY_USED),
      Map.of(this.strings.format(GRAPH), name.value()),
      DUPLICATE_GRAPH.codeName(),
      Optional.empty()
    );
  }
}
