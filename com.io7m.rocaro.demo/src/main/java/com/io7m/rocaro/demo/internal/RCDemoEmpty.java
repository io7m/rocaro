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
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.displays.RCDisplaySelectionWindowed;

import java.util.List;
import java.util.ServiceLoader;

/**
 * A demo.
 */

public final class RCDemoEmpty
  extends RCDemoAbstract
{
  /**
   * A demo.
   */

  public RCDemoEmpty()
  {
    super("empty", "The empty render graph.");
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

    final var builder = renderers.builder();
    builder.setDisplaySelection(
      new RCDisplaySelectionWindowed("RCDemoEmpty", Vector2I.of(640, 480)));

    builder.setVulkanConfiguration(this.vulkanConfiguration(context));

    try (final var renderer = builder.start()) {
      for (int index = 0; index < 60000; ++index) {
        renderer.execute(c -> {
          c.executeGraph("Empty", _ -> {

          });
        });
      }
    }

    return QCommandStatus.SUCCESS;
  }
}
