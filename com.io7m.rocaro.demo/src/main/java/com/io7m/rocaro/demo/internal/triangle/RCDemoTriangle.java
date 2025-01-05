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


package com.io7m.rocaro.demo.internal.triangle;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.lanark.core.RDottedName;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.PreparationFailed;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Preparing;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Ready;
import com.io7m.rocaro.api.graph.RCGGraphStatusType.Uninitialized;
import com.io7m.rocaro.api.graph.RCNoParameters;
import com.io7m.rocaro.demo.internal.RCDemoAbstract;
import com.io7m.rocaro.vanilla.RCAssetResolvers;

import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

/**
 * A demo.
 */

public final class RCDemoTriangle
  extends RCDemoAbstract
{
  /**
   * A demo.
   */

  public RCDemoTriangle()
  {
    super("triangle", "Show a triangle.");
  }

  @Override
  protected List<QParameterNamedType<?>> extraParameters()
  {
    return List.of();
  }

  @Override
  public QCommandStatus onExecuteDemo(
    final QCommandContextType context)
    throws Exception
  {
    final var renderers =
      ServiceLoader.load(RendererFactoryType.class)
        .findFirst()
        .orElseThrow();

    final var builder =
      renderers.builder();

    final var graphBuilder =
      builder.declareRenderGraph("TriangleDemo");
    final var frameSource =
      graphBuilder.declareOpFrameAcquire("FrameSource");
    final var frameTarget =
      graphBuilder.declareOpFramePresent("FrameTarget");

    final var frame =
      graphBuilder.declareFrameResource("Frame");

    final var triangle =
      graphBuilder.declareOperation(
        "Triangle",
        RCGRenderPassTriangle.factory(),
        RCNoParameters.NO_PARAMETERS
      );

    graphBuilder.resourceAssign(
      frameSource.frame(),
      frame
    );
    graphBuilder.connect(
      frameSource.frame(),
      triangle.frame()
    );
    graphBuilder.connect(
      triangle.frame(),
      frameTarget.frame()
    );

    builder.setAssetResolver(
      RCAssetResolvers.builder(Locale.getDefault())
        .addPackage(
          RCDemoTriangle.class.getModule(),
          new RDottedName("com.io7m.rocaro.demo.triangle"))
        .build()
    );

    builder.setDisplaySelection(
      new RCDisplaySelectionWindowed("RCDemoTriangle", Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(this.vulkanConfiguration(context));

    try (final var renderer = builder.start()) {
      for (int index = 0; index < this.frameCount(); ++index) {
        runOneFrame(renderer);
      }
    }

    return QCommandStatus.SUCCESS;
  }

  private static void runOneFrame(
    final RendererType renderer)
    throws RocaroException
  {
    renderer.executeFrame(c -> {
      c.prepare("TriangleDemo");

      switch (c.graphStatus("TriangleDemo")) {
        case final Ready _ -> {
          c.executeGraph("TriangleDemo");
        }
        case final PreparationFailed _,
             final Preparing _,
             final Uninitialized _ -> {
          c.executeGraph("Empty");
        }
      }
    });
  }
}
