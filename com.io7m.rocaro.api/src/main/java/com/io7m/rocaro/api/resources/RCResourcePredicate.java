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


package com.io7m.rocaro.api.resources;

import java.util.Objects;

/**
 * A predicate on part of a resource.
 *
 * @param description The description of the predicate
 * @param check       The actual predicate
 * @param <T>         The type of resource schematic
 */

public record RCResourcePredicate<T>(
  String description,
  CheckFunctionType<T> check)
{
  /**
   * A predicate on part of a resource.
   *
   * @param description The description of the predicate
   * @param check       The actual predicate
   */

  public RCResourcePredicate
  {
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(check, "check");
  }

  /**
   * An actual function that checks part of a schematic.
   *
   * @param <T> The type of schematic
   */

  public interface CheckFunctionType<T>
  {
    /**
     * Evaluate the check.
     *
     * @param x The input schematic
     *
     * @return Whether the part of the schematic was acceptable
     */

    CheckResultType check(T x);
  }

  /**
   * The result of checking part of a schematic.
   */

  public sealed interface CheckResultType
  {

  }

  /**
   * The part of the schematic was acceptable.
   *
   * @param value The acceptable value
   */

  public record Acceptable(
    String value)
    implements CheckResultType
  {
    /**
     * The part of the schematic was acceptable.
     *
     * @param value The acceptable value
     */

    public Acceptable
    {
      Objects.requireNonNull(value, "value");
    }
  }

  /**
   * The part of the schematic was unacceptable.
   *
   * @param value The unacceptable value
   */

  public record Unacceptable(
    String value)
    implements CheckResultType
  {
    /**
     * The part of the schematic was unacceptable.
     *
     * @param value The unacceptable value
     */

    public Unacceptable
    {
      Objects.requireNonNull(value, "value");
    }
  }
}
