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
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RendererBuilderType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplaySelectionFullscreenPrimary;
import com.io7m.rocaro.api.displays.RCDisplaySelectionType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionBuilderType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.internal.graph.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph.RCGraphDescription;
import com.io7m.rocaro.vanilla.internal.graph.RCGraphDescriptionBuilder;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRenderer;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

  private final HashMap<RCGraphName, RCGraphDescriptionBuilder> graphs;
  private final RCStrings strings;
  private final RCVersions versions;
  private final RCGLFWFacadeType glfw;
  private RendererVulkanConfiguration vulkanConfiguration;
  private RCDisplaySelectionType displaySelection;

  /**
   * A renderer builder.
   *
   * @param inStrings  The string resources
   * @param inVersions The version resources
   * @param inGLFW     The GLFW facade
   */

  public RendererBuilder(
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
      new HashMap<>();
    this.vulkanConfiguration =
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(VulkanLWJGLInstanceProvider.create())
        .build();
    this.displaySelection =
      new RCDisplaySelectionFullscreenPrimary("Rocaro");
  }

  @Override
  public RCGraphDescriptionBuilderType declareRenderGraph(
    final RCGraphName name)
    throws RCGraphDescriptionException
  {
    Objects.requireNonNull(name, "name");

    if (this.graphs.containsKey(name)) {
      throw this.errorGraphAlreadyExists(name);
    }

    final var builder = new RCGraphDescriptionBuilder(this.strings, name);
    this.graphs.put(name, builder);
    return builder;
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
    final var mainExecutor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        return Thread.ofPlatform()
          .name("com.io7m.rocaro[%s]-".formatted(freshID()), 0L)
          .unstarted(r);
      });

    try {
      return RCExecutors.executeAndWait(mainExecutor, () -> {
        LOG.debug("Starting renderer.");
        return this.createRenderer(
          RCResourceCollections.create(this.strings),
          mainExecutor
        );
      });
    } catch (final Exception e) {
      mainExecutor.close();
      throw e;
    }
  }

  private Renderer createRenderer(
    final CloseableCollectionType<RocaroException> resources,
    final ScheduledExecutorService mainExecutor)
    throws Throwable
  {
    final var tracker =
      new ExceptionTracker<RocaroException>();
    final var builtGraphDescriptions =
      this.buildGraphDescriptions(tracker);

    final var featuresRequired =
      builtGraphDescriptions.values()
        .stream()
        .map(RCGraphDescription::requiredDeviceFeatures)
        .reduce(
          VulkanPhysicalDeviceFeaturesFunctions.none(),
          VulkanPhysicalDeviceFeaturesFunctions::or
        );

    RCVulkanRendererType vulkanRenderer = null;
    try {
      vulkanRenderer = this.buildVulkanRenderer(resources, featuresRequired);
    } catch (final RocaroException e) {
      tracker.addException(e);
    }

    final var instantiatedGraphs =
      this.instantiateGraphs(tracker, builtGraphDescriptions);

    tracker.throwIfNecessary();
    return new Renderer(
      this.strings,
      resources,
      builtGraphDescriptions,
      instantiatedGraphs,
      vulkanRenderer,
      mainExecutor
    );
  }

  private Map<RCGraphName, RCGraph> instantiateGraphs(
    final ExceptionTracker<RocaroException> tracker,
    final Map<RCGraphName, RCGraphDescription> descriptions)
  {
    final var builtGraphs =
      new HashMap<RCGraphName, RCGraph>();

    for (final var description : descriptions.values()) {
      builtGraphs.put(description.name(), description.instantiate());
    }
    return Map.copyOf(builtGraphs);
  }

  private static long freshID()
  {
    return INSTANCE_IDS.incrementAndGet();
  }

  private RCVulkanRendererType buildVulkanRenderer(
    final CloseableCollectionType<RocaroException> resources,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures)
    throws RocaroException
  {
    return RCVulkanRenderer.create(
      this.strings,
      this.versions,
      this.glfw,
      resources,
      this.vulkanConfiguration,
      this.displaySelection,
      requiredDeviceFeatures
    );
  }

  private Map<RCGraphName, RCGraphDescription> buildGraphDescriptions(
    final ExceptionTracker<RocaroException> tracker)
  {
    final var builtGraphs =
      new HashMap<RCGraphName, RCGraphDescription>();

    for (final var graph : this.graphs.values()) {
      try {
        builtGraphs.put(graph.name(), graph.build());
      } catch (final RCGraphDescriptionException e) {
        tracker.addException(e);
      }
    }
    return Map.copyOf(builtGraphs);
  }

  private RCGraphDescriptionException errorGraphAlreadyExists(
    final RCGraphName name)
  {
    return new RCGraphDescriptionException(
      this.strings.format(ERROR_GRAPH_NAME_ALREADY_USED),
      Map.of(this.strings.format(GRAPH), name.value()),
      DUPLICATE_GRAPH.codeName(),
      Optional.empty()
    );
  }
}
