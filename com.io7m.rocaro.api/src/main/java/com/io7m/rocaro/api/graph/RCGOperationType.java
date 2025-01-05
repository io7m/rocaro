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


package com.io7m.rocaro.api.graph;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeaturesFunctions;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;

import java.util.Map;

/**
 * The base type of operations that may appear in the render graph.
 */

public interface RCGOperationType
{
  /**
   * @return The name of the operation
   */

  RCGOperationName name();

  /**
   * @return The operation's ports
   */

  Map<RCGPortName, RCGPortType<?>> ports();

  /**
   * @return The queue category upon which the operation executes
   */

  RCDeviceQueueCategory queueCategory();

  /**
   * @return The physical device features required for this node
   */

  default VulkanPhysicalDeviceFeatures requiredDeviceFeatures()
  {
    return VulkanPhysicalDeviceFeaturesFunctions.none();
  }

  /**
   * Prepare the operation (or continue preparation if the status indicates
   * the operation is not yet ready).
   *
   * @param context The context
   *
   * @throws RocaroException On errors
   */

  void prepare(
    RCGOperationPreparationContextType context)
    throws RocaroException;

  /**
   * @return The operation status
   */

  RCGOperationStatusType status();

  /**
   * Execute the operation.
   *
   * @param context The context
   *
   * @throws RocaroException On errors
   */

  void execute(
    RCGOperationExecutionContextType context)
    throws RocaroException;
}
