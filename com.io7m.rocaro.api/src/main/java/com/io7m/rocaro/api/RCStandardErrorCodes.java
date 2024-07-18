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


package com.io7m.rocaro.api;

import java.util.Objects;

/**
 * The standard error codes used by the package.
 */

public enum RCStandardErrorCodes
{
  /**
   * An attempt was made to create a graph with a non-unique name.
   */

  DUPLICATE_GRAPH("error-duplicate-graph"),

  /**
   * An attempt was made to create a node in a graph with a non-unique name.
   */

  DUPLICATE_NODE("error-duplicate-node"),

  /**
   * An attempt was made to connect more than one port to a given target port.
   */

  DUPLICATE_PORT_CONNECTION("error-duplicate-port-connection"),

  /**
   * An attempt was made to connect two incompatible ports.
   */

  PORTS_INCOMPATIBLE("error-incompatible-ports"),

  /**
   * A port has been left unconnected in a graph.
   */

  PORT_NOT_CONNECTED("error-unconnected-port"),

  /**
   * Creating a connection would cause a cycle in a graph.
   */

  PORT_CYCLIC_CONNECTION("error-cyclic-port-connection"),

  /**
   * Closing a resource failed.
   */

  RESOURCE_CLOSE("error-resource-close-failed"),

  /**
   * A Vulkan error occurred.
   */

  VULKAN("error-vulkan"),

  /**
   * An attempt was made to use an unsupported version of Vulkan.
   */

  VULKAN_VERSION_UNSUPPORTED("error-vulkan-version-unsupported"),

  /**
   * No suitable Vulkan device was available for the given device selection.
   */

  VULKAN_DEVICE_NONE_SUITABLE("error-vulkan-device-none-suitable"),

  /**
   * A required Vulkan extension was missing.
   */

  VULKAN_EXTENSION_MISSING("error-vulkan-extension-missing"),

  /**
   * A required Vulkan queue type was missing.
   */

  VULKAN_QUEUE_MISSING("error-vulkan-queue-missing"),

  /**
   * No suitable display was available for the given display selection.
   */

  DISPLAY_NONE_SUITABLE("error-display-none-suitable"),

  /**
   * Creation of a window failed.
   */

  DISPLAY_WINDOW_CREATION("error-display-window-creation");

  private final String codeName;

  RCStandardErrorCodes(
    final String c)
  {
    this.codeName = Objects.requireNonNull(c, "s");
  }

  /**
   * @return The underlying error code name
   */

  public String codeName()
  {
    return this.codeName;
  }
}
