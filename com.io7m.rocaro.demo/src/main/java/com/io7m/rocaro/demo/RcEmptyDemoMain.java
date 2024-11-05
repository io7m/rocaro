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


package com.io7m.rocaro.demo;

import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPractices;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesAMD;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesNVIDIA;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateCore;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateSync;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;

import java.util.ServiceLoader;

/**
 * A demo.
 */

public final class RcEmptyDemoMain
{
  private RcEmptyDemoMain()
  {

  }

  /**
   * A demo.
   *
   * @param args Command-line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    final var renderers =
      ServiceLoader.load(RendererFactoryType.class)
        .findFirst()
        .orElseThrow();

    final var builder =
      renderers.builder();

    final var graphBuilder =
      builder.declareRenderGraph("Empty");
    final var frameSource =
      graphBuilder.declareFrameSource("FrameSource");
    final var frameTarget =
      graphBuilder.declareFrameTarget("FrameTarget");

    graphBuilder.connect(
      frameSource.imageSource(),
      frameTarget.imageTarget()
    );

    builder.setDisplaySelection(
      new RCDisplaySelectionWindowed("RCStartupDemo", Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(
      RendererVulkanConfiguration.builder()
        .setInstanceProvider(VulkanLWJGLInstanceProvider.create())
        .addEnableValidation(
          new VulkanValidationValidateCore(true))
        .addEnableValidation(
          new VulkanValidationValidateSync(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPractices(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPracticesAMD(true))
        .addEnableValidation(
          new VulkanValidationValidateBestPracticesNVIDIA(true))
        .build()
    );

    try (var renderer = builder.start()) {
      for (int index = 0; index < 60000; ++index) {
        renderer.executeFrame("Empty", _ -> {

        });
      }
    }
  }
}
