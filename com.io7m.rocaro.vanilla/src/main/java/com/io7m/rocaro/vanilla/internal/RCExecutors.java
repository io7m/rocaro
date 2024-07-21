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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.rocaro.api.RocaroException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Convenience functions to perform blocking calls on other threads.
 */

public final class RCExecutors
{
  private RCExecutors()
  {

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
}
