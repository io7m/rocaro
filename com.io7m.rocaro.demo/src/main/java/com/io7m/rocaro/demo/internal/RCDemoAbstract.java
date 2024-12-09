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


package com.io7m.rocaro.demo.internal;

import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPractices;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesAMD;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateBestPracticesNVIDIA;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateCore;
import com.io7m.jcoronado.layers.khronos_validation.api.VulkanValidationValidateSync;
import com.io7m.jcoronado.lwjgl.VMALWJGLAllocatorProvider;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLInstanceProvider;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.quarrel.ext.logback.QLogback;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An abstract base class for demos.
 */

public abstract class RCDemoAbstract implements QCommandType
{
  private static final QParameterNamed01<Integer> FRAME_COUNT_LIMIT =
    new QParameterNamed01<>(
      "--frame-count-limit",
      List.of(),
      new QConstant("The maximum number of frames for which to run."),
      Optional.empty(),
      Integer.class
    );

  private static final QParameterNamed1<Boolean> VULKAN_VALIDATE_CORE =
    new QParameterNamed1<>(
      "--vulkan-validation-core",
      List.of(),
      new QConstant("Enable/disable Vulkan API validation."),
      Optional.of(Boolean.TRUE),
      Boolean.class
    );

  private static final QParameterNamed1<Boolean> VULKAN_VALIDATE_SYNC =
    new QParameterNamed1<>(
      "--vulkan-validation-sync",
      List.of(),
      new QConstant("Enable/disable Vulkan synchronization validation."),
      Optional.of(Boolean.TRUE),
      Boolean.class
    );

  private static final QParameterNamed1<Boolean> VULKAN_VALIDATE_BEST_PRACTICE =
    new QParameterNamed1<>(
      "--vulkan-validation-best-practice",
      List.of(),
      new QConstant("Enable/disable Vulkan best practices validation."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private static final QParameterNamed1<Boolean> VULKAN_VALIDATE_BEST_PRACTICE_NVIDIA =
    new QParameterNamed1<>(
      "--vulkan-validation-best-practice-nvidia",
      List.of(),
      new QConstant("Enable/disable Vulkan (NVIDIA) best practices validation."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private static final QParameterNamed1<Boolean> VULKAN_VALIDATE_BEST_PRACTICE_AMD =
    new QParameterNamed1<>(
      "--vulkan-validation-best-practice-amd",
      List.of(),
      new QConstant("Enable/disable Vulkan (AMD) best practices validation."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private static final QParameterNamed1<Boolean> RENDERDOC =
    new QParameterNamed1<>(
      "--enable-renderdoc",
      List.of(),
      new QConstant("Enable/disable RenderDoc support."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private final QCommandMetadata metadata;
  private final Logger logger;
  private int frameCount;

  /**
   * @return The demo logger
   */

  protected final Logger logger()
  {
    return this.logger;
  }

  protected RCDemoAbstract(
    final String name,
    final String description)
  {
    this.metadata =
      new QCommandMetadata(
        name,
        new QConstant(description),
        Optional.empty()
      );

    this.logger =
      LoggerFactory.getLogger(this.getClass());
  }

  @Override
  public final List<QParameterNamedType<?>> onListNamedParameters()
  {
    final var parameters = new ArrayList<QParameterNamedType<?>>();
    parameters.addAll(QLogback.parameters());
    parameters.addAll(this.vulkanParameters());
    parameters.addAll(this.extraParameters());
    return List.copyOf(parameters);
  }

  protected final List<QParameterNamedType<?>> vulkanParameters()
  {
    return List.of(
      FRAME_COUNT_LIMIT,
      VULKAN_VALIDATE_BEST_PRACTICE,
      VULKAN_VALIDATE_BEST_PRACTICE_AMD,
      VULKAN_VALIDATE_BEST_PRACTICE_NVIDIA,
      VULKAN_VALIDATE_CORE,
      VULKAN_VALIDATE_SYNC,
      RENDERDOC
    );
  }

  protected abstract List<QParameterNamedType<?>> extraParameters();

  protected final Optional<Integer> frameCountLimit(
    final QCommandContextType context)
  {
    return context.parameterValue(FRAME_COUNT_LIMIT);
  }

  @Override
  public final QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    this.frameCount =
      this.frameCountLimit(context).orElse(Integer.MAX_VALUE);

    return this.onExecuteDemo(context);
  }

  protected abstract QCommandStatus onExecuteDemo(
    QCommandContextType context)
    throws Exception;

  protected final RendererVulkanConfiguration vulkanConfiguration(
    final QCommandContextType context)
  {
    final var builder =
      RendererVulkanConfiguration.builder();

    builder.setInstanceProvider(
      VulkanLWJGLInstanceProvider.create());

    builder.setVmaAllocators(
      VMALWJGLAllocatorProvider.create());

    builder.addEnableValidation(
      new VulkanValidationValidateCore(
        context.parameterValue(VULKAN_VALIDATE_CORE)));

    builder.addEnableValidation(
      new VulkanValidationValidateSync(
        context.parameterValue(VULKAN_VALIDATE_SYNC)));

    builder.addEnableValidation(
      new VulkanValidationValidateBestPractices(
        context.parameterValue(VULKAN_VALIDATE_BEST_PRACTICE)
      ));

    builder.addEnableValidation(
      new VulkanValidationValidateBestPracticesNVIDIA(
        context.parameterValue(VULKAN_VALIDATE_BEST_PRACTICE_NVIDIA)
      ));

    builder.addEnableValidation(
      new VulkanValidationValidateBestPracticesAMD(
        context.parameterValue(VULKAN_VALIDATE_BEST_PRACTICE_AMD)
      ));

    builder.setEnableRenderDocSupport(
      context.parameterValue(RENDERDOC));

    return builder.build();
  }

  @Override
  public final QCommandMetadata metadata()
  {
    return this.metadata;
  }

  protected final void framePause()
  {
    try {
      Thread.sleep(16L * 2);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected final void longPause()
  {
    try {
      this.logger.trace("Performing long pause…");
      this.logger.trace("-".repeat(256));
      Thread.sleep(1000L);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected final int frameCount()
  {
    return this.frameCount;
  }
}
