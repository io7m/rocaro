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

package com.io7m.rocaro.rgraphc.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A path.
 *
 * @param value The path value
 */

public record RCCPath(
  List<RCCName> value)
  implements Comparable<RCCPath>
{
  /**
   * A basic name.
   *
   * @param value The name value
   */

  public RCCPath
  {
    value = List.copyOf(value);
    if (value.isEmpty()) {
      throw new IllegalArgumentException("Paths cannot be empty.");
    }
  }

  /**
   * Parse a path value.
   *
   * @param text The text
   *
   * @return The path
   *
   * @throws IllegalArgumentException On parse errors
   */

  public static RCCPath parse(
    final String text)
    throws IllegalArgumentException
  {
    return new RCCPath(
      Stream.of(text.split("\\."))
        .map(RCCName::new)
        .toList()
    );
  }

  /**
   * Construct a singleton path.
   *
   * @param name The name
   *
   * @return The path
   */

  public static RCCPath singleton(
    final RCCName name)
  {
    Objects.requireNonNull(name, "name");
    return new RCCPath(List.of(name));
  }

  @Override
  public String toString()
  {
    return this.value.stream()
      .map(RCCName::value)
      .collect(Collectors.joining("."));
  }

  /**
   * @return The path head
   */

  public RCCName head()
  {
    return this.value.get(0);
  }

  /**
   * @return The tail of this path
   */

  public Optional<RCCPath> tail()
  {
    final var elements = new LinkedList<>(this.value);
    elements.removeFirst();
    if (elements.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new RCCPath(elements));
  }

  @Override
  public int compareTo(
    final RCCPath other)
  {
    final var thatPath = other.toString();
    final var thisPath = this.toString();
    return thisPath.compareTo(thatPath);
  }

  public RCCPath plus(
    final RCCName childName)
  {
    final var newElements = new ArrayList<>(this.value);
    newElements.add(childName);
    return new RCCPath(newElements);
  }
}
