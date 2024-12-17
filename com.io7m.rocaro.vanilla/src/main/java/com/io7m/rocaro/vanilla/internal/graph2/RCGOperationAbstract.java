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


package com.io7m.rocaro.vanilla.internal.graph2;

import com.io7m.rocaro.api.graph2.RCGOperationName;
import com.io7m.rocaro.api.graph2.RCGOperationType;
import com.io7m.rocaro.api.graph2.RCGPortName;
import com.io7m.rocaro.api.graph2.RCGPortType;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * The base abstract type of operations.
 */

public abstract class RCGOperationAbstract
  implements RCGOperationType
{
  private final RCGOperationName name;
  private final HashMap<RCGPortName, RCGPortType> ports;

  /**
   * The base abstract type of operations.
   *
   * @param inName The name
   */

  public RCGOperationAbstract(
    final RCGOperationName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.ports =
      new HashMap<>();
  }

  protected final <P extends RCGPortType> P addPort(
    final P port)
  {
    if (this.ports.containsKey(port.name())) {
      throw new IllegalStateException(
        "Port name already used: %s".formatted(port.name())
      );
    }
    this.ports.put(port.name(), port);
    return port;
  }

  @Override
  public final RCGOperationName name()
  {
    return this.name;
  }

  @Override
  public final List<RCGPortType> ports()
  {
    return List.copyOf(this.ports.values());
  }
}
