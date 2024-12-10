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
import com.io7m.rocaro.api.RocaroException;
import com.io7m.seltzer.api.SStructuredErrorExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabels.serializeLabels;

/**
 * A strict single-thread executor.
 */

public final class RCExecutorOne implements RCExecutorType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCExecutorOne.class);

  private final AtomicBoolean closed;
  private final LinkedBlockingQueue<Runnable> queue;
  private final RCThreadLabel label;
  private final RCThreadLabel[] labels;
  private Thread thread;

  private RCExecutorOne(
    final RCThreadLabel inLabel,
    final RCThreadLabel[] inLabels)
  {
    this.label =
      Objects.requireNonNull(inLabel, "label");
    this.labels =
      Objects.requireNonNull(inLabels, "labels");
    this.closed =
      new AtomicBoolean(false);
    this.queue =
      new LinkedBlockingQueue<Runnable>();
  }

  /**
   * Create a new executor.
   *
   * @param id       The renderer ID
   * @param nameBase The base executor name
   * @param label    The primary thread label
   * @param labels   The supplementary thread labels
   *
   * @return An executor
   */

  public static RCExecutorType create(
    final RCRendererID id,
    final String nameBase,
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    final var x =
      new RCExecutorOne(label, labels);

    final var thread =
      new Thread(null, x::run);

    final var name =
      String.format(
        "com.io7m.rocaro[%s].%s[%s]",
        Long.toUnsignedString(id.value(), 16),
        nameBase,
        Long.toUnsignedString(thread.threadId())
      );

    thread.setName(name);
    thread.setDaemon(true);
    thread.setUncaughtExceptionHandler((_, e) -> {
      LOG.debug("Uncaught exception: ", e);
    });

    x.setThread(thread);
    x.start();
    return x;
  }

  private void start()
  {
    this.thread.start();
  }

  private void setThread(
    final Thread inThread)
  {
    this.thread = Objects.requireNonNull(inThread, "thread");
  }

  private void run()
  {
    RCThreadLabels.LABELS.set(serializeLabels(this.label, this.labels));

    while (!this.closed.get()) {
      try {
        final var runnable =
          this.queue.poll(16L, TimeUnit.MILLISECONDS);

        if (runnable != null) {
          runnable.run();
        }
      } catch (final Throwable e) {
        LOG.debug("Uncaught exception: ", e);
      }
    }
  }

  @Override
  public void execute(
    final Runnable command)
  {
    if (!this.closed.get()) {
      this.queue.add(Objects.requireNonNull(command, "command"));
    } else {
      throw new IllegalStateException("Executor is closed.");
    }
  }

  @Override
  public void close()
  {
    this.closed.set(true);
  }

  @Override
  public <T> T executeAndWait(
    final RCPartialFunctionType<T> runnable)
    throws RocaroException
  {
    final var future =
      new CompletableFuture<T>();

    this.execute(() -> {
      try {
        future.complete(runnable.execute());
      } catch (final Throwable e) {
        LOG.debug("executeAndWait: ", e);
        future.completeExceptionally(e);
      }
    });

    try {
      return future.get();
    } catch (final InterruptedException e) {
      throw new RCExecutionException(
        e,
        Map.of(),
        "error-interrypted",
        Optional.empty()
      );
    } catch (final ExecutionException e) {
      final var cause = e.getCause();
      switch (cause) {
        case final RocaroException re -> {
          throw re;
        }
        case final SStructuredErrorExceptionType<?> re -> {
          throw new RCExecutionException(
            e,
            re.attributes(),
            re.errorCode().toString(),
            re.remediatingAction()
          );
        }
        case null, default -> {
          throw new RCExecutionException(
            e,
            Map.of(),
            "error-unknown",
            Optional.empty()
          );
        }
      }
    }
  }
}
