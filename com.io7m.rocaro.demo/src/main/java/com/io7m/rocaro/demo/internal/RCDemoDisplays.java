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

import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.rocaro.api.displays.RCDisplayServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;

/**
 * A demo.
 */

public final class RCDemoDisplays
  extends RCDemoAbstract
  implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCDemoDisplays.class);

  /**
   * A demo.
   */

  public RCDemoDisplays()
  {
    super("displays", "List displays.");
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var displayService =
      ServiceLoader.load(RCDisplayServiceType.class)
        .findFirst()
        .orElseThrow();

    final var displays =
      displayService.displays();

    for (final var display : displays) {
      LOG.debug(
        "Display: {} {}x{}mm (Primary: {})",
        display.name(),
        display.physicalWidthMM(),
        display.physicalHeightMM(),
        display.isPrimary()
      );

      display.displayModes()
        .stream()
        .sorted()
        .forEach(mode -> {
          LOG.debug(
            "  Mode: {}x{} R{}G{}B{} {}hz",
            mode.widthPixels(),
            mode.heightPixels(),
            mode.redBits(),
            mode.greenBits(),
            mode.blueBits(),
            mode.refreshRateHZ()
          );
        });
    }
    return QCommandStatus.SUCCESS;
  }

  @Override
  protected List<QParameterNamedType<?>> extraParameters()
  {
    return List.of();
  }
}
