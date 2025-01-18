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


package com.io7m.rocaro.rgraphc.internal.untyped;

import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public final class RCUDeclOperation
  extends RCUGraphElement
  implements RCUGraphElementType
{
  private final RCDeviceQueueCategory queueCategory;
  private final RCCName name;
  private final TreeMap<RCCName, RCUDeclPortType> ports;
  private final SortedMap<RCCName, RCUDeclPortType> portsRead;

  public RCUDeclOperation(
    final RCDeviceQueueCategory inQueueCategory,
    final RCCName inName)
  {
    this.queueCategory =
      Objects.requireNonNull(inQueueCategory, "queueCategory");
    this.name =
      Objects.requireNonNull(inName, "name");
    this.ports =
      new TreeMap<>();
    this.portsRead =
      Collections.unmodifiableSortedMap(this.ports);
  }

  public RCDeviceQueueCategory queueCategory()
  {
    return this.queueCategory;
  }

  public RCCName name()
  {
    return this.name;
  }

  public SortedMap<RCCName, RCUDeclPortType> ports()
  {
    return this.portsRead;
  }

  public void addPort(
    final RCUDeclPortType port)
  {
    if (this.ports.containsKey(port.name())) {
      throw new IllegalStateException(
        "Port name %s is already used."
          .formatted(port.name())
      );
    }
    this.ports.put(port.name(), port);
  }
}
