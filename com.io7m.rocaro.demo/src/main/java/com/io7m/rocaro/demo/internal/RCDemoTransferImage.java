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

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RCFrameNumber;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;
import com.io7m.rocaro.api.transfers.RCTransferImageColorBasic;
import com.io7m.rocaro.api.transfers.RCTransferServiceType;
import com.io7m.rocaro.vanilla.internal.frames.RCFrameServiceType;

import java.math.BigInteger;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.io7m.jcoronado.api.VulkanFormat.VK_FORMAT_R8_UNORM;
import static com.io7m.jcoronado.api.VulkanImageLayout.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static com.io7m.rocaro.api.devices.RCDeviceQueueCategory.GRAPHICS;

/**
 * A demo.
 */

public final class RCDemoTransferImage
  extends RCDemoAbstract
{
  /**
   * A demo.
   */

  public RCDemoTransferImage()
  {
    super("transfer-image", "Test image transfers.");
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

    final var builder = renderers.builder();
    builder.setDisplaySelection(
      new RCDisplaySelectionWindowed("RCDemoStartup", Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(this.vulkanConfiguration(context));

    try (final var r = builder.start()) {
      final var transfers =
        r.requireService(RCTransferServiceType.class);
      final var frames =
        r.requireService(RCFrameServiceType.class);

      frames.beginNewFrame(new RCFrameInformation(
        new RCFrameNumber(BigInteger.ZERO),
        new RCFrameIndex(0)
      ));

      final var future =
        transfers.transfer(
          RCTransferImageColorBasic.builder()
            .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            .setFormat(VK_FORMAT_R8_UNORM)
            .setId(UUID.randomUUID())
            .setName("example-image")
            .setSize(Vector2I.of(128, 128))
            .setTargetQueue(GRAPHICS)
            .setDataCopier(target -> target.fill((byte) 0x7f))
            .build()
        );

      this.framePause();

      frames.beginNewFrame(new RCFrameInformation(
        new RCFrameNumber(BigInteger.ONE),
        new RCFrameIndex(1)
      ));

      final var i = future.get(2L, TimeUnit.SECONDS);
      final var logger = this.logger();
      logger.debug("Transfer future complete: Image {}", i);
    }

    return QCommandStatus.SUCCESS;
  }
}
