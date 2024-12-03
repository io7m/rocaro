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
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceType;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RCFrameNumber;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RendererFrameBuilderProcedureType;
import com.io7m.rocaro.api.RendererFrameBuilderType;
import com.io7m.rocaro.api.RendererGraphProcedureType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.graph.RCGStatusType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.graph.RCGraphName;
import com.io7m.rocaro.vanilla.internal.frames.RCFrameServiceType;
import com.io7m.rocaro.vanilla.internal.graph.RCGraph;
import com.io7m.rocaro.vanilla.internal.graph.RCGraphDescription;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutors;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameContextType;
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

  private final RPServiceDirectoryType services;
  private final RCStrings strings;
  private final RCVulkanRendererType vulkanRenderer;
  private final RCFrameServiceType frameService;
  private final CloseableCollectionType<RocaroException> resources;
  private final Map<RCGraphName, RCGraphDescription> graphDescriptions;
  private final Map<RCGraphName, RCGraph> graphs;
  private final ScheduledExecutorService mainExecutor;
  private final AtomicBoolean closed;
  private RCFrameNumber frameNumber;
  private RCRendererID id;

  Renderer(
    final RPServiceDirectoryType inServices,
    final RCStrings inStrings,
    final RCVulkanRendererType inVulkanRenderer,
    final RCFrameServiceType inFrameService,
    final CloseableCollectionType<RocaroException> inResources,
    final Map<RCGraphName, RCGraphDescription> inGraphDescriptions,
    final Map<RCGraphName, RCGraph> inGraphs,
    final ScheduledExecutorService inMainExecutor,
    final RCRendererID inId)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.vulkanRenderer =
      Objects.requireNonNull(inVulkanRenderer, "vulkanRenderer");
    this.frameService =
      Objects.requireNonNull(inFrameService, "inFrameService");
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.graphDescriptions =
      Map.copyOf(inGraphDescriptions);
    this.graphs =
      Map.copyOf(inGraphs);
    this.mainExecutor =
      Objects.requireNonNull(inMainExecutor, "mainExecutor");
    this.id =
      Objects.requireNonNull(inId, "id");
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
          this.vulkanRenderer.device()
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
  public RCRendererID id()
  {
    return this.id;
  }

  @Override
  public <T extends RPServiceType> T requireService(
    final Class<T> clazz)
  {
    return this.services.requireService(
      Objects.requireNonNull(clazz, "clazz")
    );
  }

  @Override
  public <T extends RPServiceType> Optional<T> optionalService(
    final Class<T> clazz)
  {
    return this.services.optionalService(
      Objects.requireNonNull(clazz, "clazz")
    );
  }

  @Override
  public void execute(
    final RendererFrameBuilderProcedureType f)
    throws RocaroException
  {
    Objects.requireNonNull(f, "f");

    RCExecutors.executeAndWait(
      this.mainExecutor,
      () -> {
        this.doExecute(f);
        return RCUnit.UNIT;
      }
    );
  }

  private void doExecute(
    final RendererFrameBuilderProcedureType f)
    throws RocaroException, TimeoutException
  {
    try {
      final var frameIndex =
        this.frameNumber.toFrameIndex(
          this.vulkanRenderer.maximumFramesInFlight()
        );

      final var frameInformation =
        new RCFrameInformation(this.frameNumber, frameIndex);

      try (final var frameContext =
             this.vulkanRenderer.acquireFrame(frameIndex)) {
        this.frameService.beginNewFrame(frameInformation);

        f.execute(
          new FrameBuilder(
            this,
            frameInformation,
            frameContext
          )
        );
      }
    } finally {
      this.frameNumber = this.frameNumber.next();
    }
  }

  private static final class FrameBuilder
    implements RendererFrameBuilderType
  {
    private final Renderer renderer;
    private final RCFrameInformation frameInformation;
    private final RCVulkanFrameContextType frameContext;

    FrameBuilder(
      final Renderer inRenderer,
      final RCFrameInformation inFrameInformation,
      final RCVulkanFrameContextType inFrameContext)
    {
      this.renderer =
        Objects.requireNonNull(inRenderer, "renderer");
      this.frameInformation =
        Objects.requireNonNull(inFrameInformation, "frameInformation");
      this.frameContext =
        Objects.requireNonNull(inFrameContext, "frameContext");
    }

    @Override
    public RCFrameInformation frameInformation()
    {
      return this.frameInformation;
    }

    @Override
    public void prepare(
      final RCGraphName graphName)
      throws RocaroException
    {
      this.renderer.graph(graphName)
        .prepare(this.frameInformation);
    }

    @Override
    public RCGStatusType graphStatus(
      final RCGraphName graphName)
      throws RocaroException
    {
      return this.renderer.graph(graphName).status();
    }

    @Override
    public void executeGraph(
      final RCGraphName graphName,
      final RendererGraphProcedureType f)
      throws RocaroException
    {
      this.renderer.graph(graphName)
        .evaluate(
          this.frameInformation,
          this.frameContext,
          this.renderer.strings
        );
    }
  }

  private RCGraph graph(
    final RCGraphName name)
    throws RocaroException
  {
    final var graph = this.graphs.get(name);
    if (graph == null) {
      throw errorNoSuchGraph(this.strings, name);
    }
    return graph;
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
}
