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

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RCFrameNumber;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RendererFrameFunctionType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.internal.graph.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph.RCGraphDescription;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.rocaro.api.RCStandardErrorCodes.NONEXISTENT_GRAPH;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GRAPH_NONEXISTENT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.GRAPH;

/**
 * A renderer.
 */

public final class Renderer
  extends RCObject
  implements RendererType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Renderer.class);

  private final RCStrings strings;
  private final CloseableCollectionType<RocaroException> resources;
  private final Map<RCGraphName, RCGraphDescription> graphDescriptions;
  private final Map<RCGraphName, RCGraph> graphs;
  private final RCVulkanRendererType vulkanRenderer;
  private final ScheduledExecutorService mainExecutor;
  private final AtomicBoolean closed;
  private RCFrameNumber frameNumber;

  Renderer(
    final RCStrings inStrings,
    final CloseableCollectionType<RocaroException> inResources,
    final Map<RCGraphName, RCGraphDescription> inGraphDescriptions,
    final Map<RCGraphName, RCGraph> inGraphs,
    final RCVulkanRendererType inVulkanRenderer,
    final ScheduledExecutorService inMainExecutor)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.graphDescriptions =
      Objects.requireNonNull(inGraphDescriptions, "graphs");
    this.graphs =
      Objects.requireNonNull(inGraphs, "inGraphs");
    this.vulkanRenderer =
      Objects.requireNonNull(inVulkanRenderer, "vulkanRenderer");
    this.mainExecutor =
      Objects.requireNonNull(inMainExecutor, "mainExecutor");

    this.frameNumber =
      RCFrameNumber.first();
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void close()
    throws RocaroException
  {
    if (this.closed.compareAndSet(false, true)) {
      LOG.debug("Shutting down renderer.");
      RCExecutors.executeAndWait(this.mainExecutor, () -> {
        LOG.debug("Closing renderer.");

        try {
          LOG.debug("Waiting for device to idle.");
          this.vulkanRenderer.logicalDevice()
            .device()
            .waitIdle();
        } catch (final VulkanException e) {
          LOG.error("Failed to wait for device: ", e);
        }

        this.resources.close();
        return RCUnit.UNIT;
      });
      LOG.debug("Shutting down renderer main thread.");
      this.mainExecutor.close();
      LOG.debug("Renderer closed.");
    }
  }

  @Override
  public void executeFrame(
    final RCGraphName graphName,
    final RendererFrameFunctionType f)
    throws RocaroException
  {
    Objects.requireNonNull(graphName, "graphName");
    Objects.requireNonNull(f, "f");

    final var graph = this.graphs.get(graphName);
    if (graph == null) {
      throw errorNoSuchGraph(this.strings, graphName);
    }

    RCExecutors.executeAndWait(
      this.mainExecutor,
      () -> {
        this.doExecuteFrame(graph, f);
        return RCUnit.UNIT;
      }
    );
  }

  private static RocaroException errorNoSuchGraph(
    final RCStrings strings,
    final RCGraphName graphName)
  {
    return new RCGraphDescriptionException(
      strings.format(ERROR_GRAPH_NONEXISTENT),
      Map.ofEntries(
        Map.entry(strings.format(GRAPH), graphName.value())
      ),
      NONEXISTENT_GRAPH.codeName(),
      Optional.empty()
    );
  }

  private void doExecuteFrame(
    final RCGraph graph,
    final RendererFrameFunctionType f)
    throws RocaroException, TimeoutException
  {
    try {
      final var frameIndex =
        this.frameNumber.toFrameIndex(
          this.vulkanRenderer.maximumFramesInFlight()
        );

      final var frameInformation =
        new RCFrameInformation(this.frameNumber, frameIndex);

      try (var frameContext = this.vulkanRenderer.acquireFrame(frameIndex)) {
        graph.evaluate(frameInformation, frameContext);
      }
    } finally {
      this.frameNumber = this.frameNumber.next();
    }
  }

}
