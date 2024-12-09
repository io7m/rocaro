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


package com.io7m.rocaro.vanilla.internal.threading;

import com.io7m.rocaro.api.RCRendererID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Convenience functions to perform blocking calls on other threads.
 */

public final class RCExecutors
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCExecutors.class);

  private RCExecutors()
  {

  }

  /**
   * Create a new single-threaded executor with a reasonable naming scheme
   * that uses platform threads.
   *
   * @param name   The subsystem name
   * @param id     The renderer ID
   * @param label  The primary thread label
   * @param labels The other thread labels
   *
   * @return A new executor
   */

  public static RCExecutorType createPlatformExecutor(
    final String name,
    final RCRendererID id,
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(labels, "labels");

    return RCExecutorOne.create(
      id,
      name,
      label,
      labels
    );
  }

  /**
   * Create a new thread-per-task executor with a reasonable naming scheme
   * that uses virtual threads.
   *
   * @param name   The subsystem name
   * @param id     The renderer ID
   * @param label  The primary thread label
   * @param labels The other thread labels
   *
   * @return A new executor
   */

  public static ExecutorService createVirtualExecutor(
    final String name,
    final RCRendererID id,
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(labels, "labels");

    final var nameFormat =
      "com.io7m.rocaro[%s].%s-"
        .formatted(Long.toUnsignedString(id.value(), 16), name);

    return Executors.newThreadPerTaskExecutor(r -> {
      final Runnable w = () -> {
        RCThreadLabels.LABELS.set(RCThreadLabels.serializeLabels(label, labels));
        r.run();
      };

      return Thread.ofVirtual()
        .name(nameFormat, 0L)
        .unstarted(w);
    });
  }
}
