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

import com.io7m.quarrel.core.QApplication;
import com.io7m.quarrel.core.QApplicationMetadata;
import com.io7m.quarrel.core.QApplicationType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QStringType;
import com.io7m.rocaro.demo.internal.RCDemoDisplays;
import com.io7m.rocaro.demo.internal.RCDemoEmpty;
import com.io7m.rocaro.demo.internal.RCDemoStartup;
import com.io7m.rocaro.demo.internal.RCDemoTransferImage;
import com.io7m.rocaro.demo.internal.triangle.RCDemoTriangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The main entry point.
 */

public final class RCDemoMain implements Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCDemoMain.class);

  private final List<String> args;
  private final QApplicationType application;
  private int exitCode;

  /**
   * The main entry point.
   *
   * @param inArgs Command-line arguments
   */

  public RCDemoMain(
    final String[] inArgs)
  {
    this.args =
      Objects.requireNonNull(List.of(inArgs), "Command line arguments");

    final var metadata =
      new QApplicationMetadata(
        "rocaro",
        "com.io7m.rocaro",
        RCDVersion.MAIN_VERSION,
        RCDVersion.MAIN_BUILD,
        "The rocaro demo command-line application.",
        Optional.of(URI.create("https://www.io7m.com/software/rocaro/"))
      );

    final var builder =
      QApplication.builder(metadata);

    {
      final var g =
        builder.createCommandGroup(
          new QCommandMetadata(
            "demo",
            new QStringType.QConstant("The demos."),
            Optional.empty())
        );
      g.addCommand(new RCDemoDisplays());
      g.addCommand(new RCDemoEmpty());
      g.addCommand(new RCDemoStartup());
      g.addCommand(new RCDemoTriangle());
      g.addCommand(new RCDemoTransferImage());
    }

    builder.allowAtSyntax(true);

    this.application = builder.build();
    this.exitCode = 0;
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(
    final String[] args)
  {
    System.exit(mainExitless(args));
  }

  /**
   * The main (exitless) entry point.
   *
   * @param args Command line arguments
   *
   * @return The exit code
   */

  public static int mainExitless(
    final String[] args)
  {
    final RCDemoMain cm = new RCDemoMain(args);
    cm.run();
    return cm.exitCode();
  }

  /**
   * @return The quarrel application
   */

  public QApplicationType application()
  {
    return this.application;
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exitCode;
  }

  @Override
  public void run()
  {
    this.exitCode = this.application.run(LOG, this.args).exitCode();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[RCDemoMain 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
