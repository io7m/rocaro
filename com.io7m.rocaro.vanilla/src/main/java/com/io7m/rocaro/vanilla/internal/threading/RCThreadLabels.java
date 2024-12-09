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

import java.util.List;

/**
 * Functions over thread labels.
 */

public final class RCThreadLabels
{
  private RCThreadLabels()
  {

  }

  /**
   * A thread-local value used to hold the set of labels for the current
   * thread. The labels are flattened into an integer bitmask for
   * efficiency/premature optimization.
   */

  static final ThreadLocal<Integer> LABELS =
    ThreadLocal.withInitial(() -> 0);

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

  static int serializeLabels(
    final RCThreadLabel label,
    final RCThreadLabel... labels)
  {
    var x = label.bit();
    for (final var labelNow : labels) {
      x |= labelNow.bit();
    }
    return x;
  }
}
