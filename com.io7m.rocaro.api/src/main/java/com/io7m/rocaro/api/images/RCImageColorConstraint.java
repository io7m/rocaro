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
 * The main color constraint.
 *
 * @param exactFormat  The exact format, if any
 * @param channels     The required channels, if any
 * @param capabilities The minimum set of required capabilities
 */

public record RCImageColorConstraint(
  Optional<RCImageColorFormat> exactFormat,
  Optional<RCImageColorChannels> channels,
  Set<RCImageFormatCapability> capabilities)
  implements RCImageColorConstraintType
{
  private static final RCImageColorConstraintType ANY =
    new RCImageColorConstraint(
      Optional.empty(),
      Optional.empty(),
      Set.of()
    );

  /**
   * The main color constraint.
   *
   * @param exactFormat  The exact format, if any
   * @param channels     The required channels, if any
   * @param capabilities The minimum set of required capabilities
   */

  public RCImageColorConstraint
  {
    Objects.requireNonNull(exactFormat, "exactFormat");
    Objects.requireNonNull(channels, "channels");
    Objects.requireNonNull(capabilities, "capabilities");

    if (exactFormat.isPresent()) {
      channels = Optional.of(exactFormat.get().channels());
      capabilities = exactFormat.get().capabilities();
    }
  }

  /**
   * @param format The format
   *
   * @return A constraint that requires exactly the given format
   */

  public static RCImageColorConstraintType exactColorFormat(
    final RCImageColorFormat format)
  {
    return new RCImageColorConstraint(
      Optional.of(format),
      Optional.empty(),
      Set.of()
    );
  }

  /**
   * @param channels The channels
   *
   * @return A constraint that requires any image with the given channels
   */

  public static RCImageColorConstraintType anyColorWithChannels(
    final RCImageColorChannels channels)
  {
    return new RCImageColorConstraint(
      Optional.empty(),
      Optional.of(channels),
      Set.of()
    );
  }

  /**
   * @param capabilities The capabilities
   *
   * @return A constraint that requires any image with at least the given capabilities
   */

  public static RCImageColorConstraintType anyColorWithCapabilities(
    final RCImageFormatCapability... capabilities)
  {
    return new RCImageColorConstraint(
      Optional.empty(),
      Optional.empty(),
      Set.of(capabilities)
    );
  }

  /**
   * @return A color image constraint that accepts anything
   */

  public static RCImageColorConstraintType anyColorFormat()
  {
    return ANY;
  }

  @Override
  public boolean isSatisfiedBy(
    final RCGPortDataConstraintType<?> other)
  {
    if (other instanceof final RCImageColorConstraintType colorOther) {
      return this.isExactFormatOK(colorOther.exactFormat())
             && this.areChannelsOK(colorOther.channels())
             && this.areCapabilitiesOK(this.capabilitiesOf(colorOther));
    }
    return false;
  }

  @Override
  public boolean isValid(
    final RCImageColorType value)
  {
    final var valueFormat =
      value.format();
    final var valueChannels =
      valueFormat.channels();
    final var valueCapabilities =
      valueFormat.capabilities();

    return this.exactFormat.map(f -> value.format() == f)
      .orElseGet(() -> {
        return this.areChannelsOK(Optional.of(valueChannels))
               && this.areCapabilitiesOK(valueCapabilities);
      })
      .booleanValue();
  }

  private Set<RCImageFormatCapability> capabilitiesOf(
    final RCImageColorConstraintType colorOther)
  {
    return colorOther.exactFormat()
      .map(RCImageColorFormat::capabilities)
      .orElse(colorOther.capabilities());
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
    if (this.channels.isPresent()) {
      if (!text.isEmpty()) {
        text.append(" ∧ ");
      }
      text.append("(channels ⊇ ");
      text.append(this.channels.get());
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

  private boolean isExactFormatOK(
    final Optional<RCImageColorFormat> formatOther)
  {
    return this.exactFormat.flatMap(formatThis -> {
        return formatOther.map(formatThat -> {
          return formatThis == formatThat;
        });
      })
      .orElse(TRUE)
      .booleanValue();
  }

  private boolean areCapabilitiesOK(
    final Set<RCImageFormatCapability> capabilitiesOther)
  {
    return capabilitiesOther.containsAll(this.capabilities);
  }

  private boolean areChannelsOK(
    final Optional<RCImageColorChannels> channelsOther)
  {
    return this.channels.flatMap(channelsThis -> {
        return channelsOther.map(channelsThat -> {
          return channelsThat.isSupersetOf(channelsThis);
        });
      })
      .orElse(TRUE)
      .booleanValue();
  }
}
