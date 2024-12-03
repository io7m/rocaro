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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RocaroException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Convenience functions to perform blocking calls on other threads.
 */

public final class RCExecutors
{
  /**
   * A thread-local value used to hold the set of labels for the current
   * thread. The labels are flattened into an integer bitmask for
   * efficiency/premature optimization.
   */

  private static final ThreadLocal<Integer> LABELS =
    ThreadLocal.withInitial(() -> 0);

  private RCExecutors()
  {

  }

  /**
   * Check that the current thread has all the given labels.
   *
   * @param labels The labels
   */

  public static void checkThreadLabelsAll(
    final RCThreadLabel... labels)
  {
    final var bits = LABELS.get().intValue();

    for (final var label : labels) {
      if ((bits & label.bit()) == label.bit()) {
        continue;
      }
      Preconditions.checkPreconditionV(
        false,
        "The current thread '%s' must have label %s.",
        Thread.currentThread().getName(),
        label
      );
    }
  }

  /**
   * Check that the current thread has any of the given labels.
   *
   * @param labels The labels
   */

  public static void checkThreadLabelsAny(
    final RCThreadLabel... labels)
  {
    final var bits = LABELS.get().intValue();

    for (final var label : labels) {
      if ((bits & label.bit()) == label.bit()) {
        return;
      }
    }

    Preconditions.checkPreconditionV(
      false,
      "The current thread '%s' must have one of the labels %s.",
      Thread.currentThread().getName(),
      List.of(labels)
    );
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

  public static ScheduledExecutorService createPlatformExecutor(
    final String name,
    final RCRendererID id,
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(labels, "labels");

    final var nameFormat =
      "com.io7m.rocaro.%s[%s]-"
        .formatted(name, Long.toUnsignedString(id.value(), 16));

    return Executors.newSingleThreadScheduledExecutor(r -> {
      final Runnable w = () -> {
        LABELS.set(serializeLabels(label, labels));
        r.run();
      };

      return Thread.ofPlatform()
        .name(nameFormat, 0L)
        .unstarted(w);
    });
  }

  private static int serializeLabels(
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    var x = label.bit();
    for (final var labelNow : labels) {
      x |= labelNow.bit();
    }
    return x;
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
      "com.io7m.rocaro.%s[%s]-"
        .formatted(name, Long.toUnsignedString(id.value(), 16));

    return Executors.newThreadPerTaskExecutor(r -> {
      final Runnable w = () -> {
        LABELS.set(serializeLabels(label, labels));
        r.run();
      };

      return Thread.ofVirtual()
        .name(nameFormat, 0L)
        .unstarted(w);
    });
  }

  /**
   * Execute a function on the given executor.
   *
   * @param executor The executor
   * @param runnable The function
   * @param <T>      The type of results
   *
   * @return The function result
   *
   * @throws RocaroException On errors
   */

  public static <T> T executeAndWait(
    final ExecutorService executor,
    final PartialFunctionType<T> runnable)
    throws RocaroException
  {
    final var future =
      new CompletableFuture<T>();

    executor.execute(() -> {
      try {
        future.complete(runnable.execute());
      } catch (final Throwable e) {
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
        case null -> {
          throw new RCExecutionException(
            e,
            Map.of(),
            "error-unknown",
            Optional.empty()
          );
        }
        default -> {
          throw new RCExecutionException(
            cause,
            Map.of(),
            "error-unknown",
            Optional.empty()
          );
        }
      }
    }
  }

  /**
   * The type of partial functions.
   *
   * @param <T> The type of returned values
   */

  public interface PartialFunctionType<T>
  {
    /**
     * Execute the function.
     *
     * @return The result
     *
     * @throws Throwable On errors
     */

    T execute()
      throws Throwable;
  }
}
