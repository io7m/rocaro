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


package com.io7m.rocaro.api.images;

import com.io7m.rocaro.api.graph.RCGPortDataConstraintType;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.Boolean.TRUE;

/**
 * A constraint on depth images.
 *
 * @param exactFormat  The exact format, if any
 * @param capabilities The minimum set of required capabilities
 */

public record RCImageDepthConstraint(
  Optional<RCImageDepthFormatType> exactFormat,
  Set<RCImageFormatCapability> capabilities)
  implements RCImageDepthConstraintType
{
  /**
   * A constraint on depth images.
   *
   * @param exactFormat  The exact format, if any
   * @param capabilities The minimum set of required capabilities
   */

  public RCImageDepthConstraint
  {
    Objects.requireNonNull(exactFormat, "exactFormat");
    Objects.requireNonNull(capabilities, "capabilities");
  }

  private static final RCImageDepthConstraintType ANY =
    new RCImageDepthConstraint(Optional.empty(), Set.of());

  /**
   * @return A depth image constraint that accepts anything
   */

  public static RCImageDepthConstraintType anyDepthImage()
  {
    return ANY;
  }

  /**
   * @param capabilities The capabilities
   *
   * @return A constraint that requires any depth image with at least the given capabilities
   */

  public static RCImageDepthConstraintType anyDepthWithCapabilities(
    final RCImageFormatCapability... capabilities)
  {
    return new RCImageDepthConstraint(
      Optional.empty(),
      Set.of(capabilities)
    );
  }

  /**
   * @param format The format
   *
   * @return A constraint that requires exactly the given format
   */

  public static RCImageDepthConstraintType exactDepth(
    final RCImageDepthFormatType format)
  {
    return new RCImageDepthConstraint(Optional.of(format), Set.of());
  }

  @Override
  public boolean isSatisfiedBy(
    final RCGPortDataConstraintType<?> other)
  {
    if (other instanceof final RCImageDepthConstraintType depthOther) {
      return this.isExactFormatOK(depthOther.exactFormat())
             && this.areCapabilitiesOK(depthOther.capabilities());
    }
    return false;
  }

  @Override
  public boolean isValid(
    final RCImageDepthType value)
  {
    final var valueFormat =
      value.format();
    final var valueCapabilities =
      valueFormat.capabilities();

    return this.exactFormat.map(f -> Objects.equals(value.format(), f))
      .orElseGet(() -> {
        return this.areCapabilitiesOK(valueCapabilities);
      })
      .booleanValue();
  }

  @Override
  public String explain()
  {
    final var text = new StringBuilder(128);
    if (this.exactFormat.isPresent()) {
      text.append("(format ≡ ");
      text.append(this.exactFormat.get());
      text.append(")");
    }
    if (!text.isEmpty()) {
      text.append(" ∧ ");
    }
    text.append("(capabilities ⊇ ");
    text.append(this.capabilities);
    text.append(")");
    return text.toString();
  }

  private boolean areCapabilitiesOK(
    final Set<RCImageFormatCapability> capabilitiesOther)
  {
    return capabilitiesOther.containsAll(this.capabilities);
  }

  private boolean isExactFormatOK(
    final Optional<RCImageDepthFormatType> formatOther)
  {
    return this.exactFormat.flatMap(formatThis -> {
        return formatOther.map(formatThis::equals);
      })
      .orElse(TRUE)
      .booleanValue();
  }
}
