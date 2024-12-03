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
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.RendererType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.api.graph.RCGStatusFailed;
import com.io7m.rocaro.api.graph.RCGStatusInProgress;
import com.io7m.rocaro.api.graph.RCGStatusReady;
import com.io7m.rocaro.api.graph.RCGStatusUninitialized;
import com.io7m.rocaro.demo.internal.RCDemoAbstract;
import com.io7m.rocaro.vanilla.RCAssetResolvers;

import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import static com.io7m.rocaro.demo.internal.triangle.RCRenderPassTriangles.RENDER_PASS_TRIANGLE;

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
  public QCommandStatus onExecute(
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
      graphBuilder.declareFrameSource("FrameSource");
    final var frameTarget =
      graphBuilder.declareFrameTarget("FrameTarget");

    final var triangle =
      graphBuilder.declare(
        "Triangle",
        RCUnit.UNIT,
        RENDER_PASS_TRIANGLE
      );

    graphBuilder.connect(
      frameSource.imageSource(),
      triangle.targetPort("Image")
    );

    graphBuilder.connect(
      triangle.sourcePort("Image"),
      frameTarget.imageTarget()
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

    final var frameLimit =
      this.frameCountLimit(context);

    try (final var renderer = builder.start()) {
      if (frameLimit.isPresent()) {
        final var limit = frameLimit.get();
        for (int index = 0; index < limit; ++index) {
          runOneFrame(renderer);
        }
      } else {
        while (true) {
          runOneFrame(renderer);
        }
      }
    }

    return QCommandStatus.SUCCESS;
  }

  private static void runOneFrame(
    final RendererType renderer)
    throws RocaroException
  {
    renderer.execute(c -> {
      c.prepare("TriangleDemo");

      switch (c.graphStatus("TriangleDemo")) {
        case final RCGStatusReady _ -> {
          c.executeGraph("TriangleDemo");
        }
        case final RCGStatusInProgress _,
             final RCGStatusUninitialized _,
             final RCGStatusFailed _ -> {
          c.executeGraph("Empty");
        }
      }
    });
  }
}
