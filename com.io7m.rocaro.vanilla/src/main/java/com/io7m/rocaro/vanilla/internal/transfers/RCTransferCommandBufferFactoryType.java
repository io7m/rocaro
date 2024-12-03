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


package com.io7m.rocaro.vanilla.internal.transfers;

import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanQueueType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RocaroException;

/**
 * A factory of command buffers for transfers.
 */

public interface RCTransferCommandBufferFactoryType
{
  /**
   * Create a command buffer for the given queue.
   *
   * @param resources The resource collection
   * @param queue     The queue
   * @param name      The buffer name
   *
   * @return The buffer
   *
   * @throws RocaroException On errors
   */

  VulkanCommandBufferType commandBufferForQueue(
    CloseableCollectionType<RocaroException> resources,
    VulkanQueueType queue,
    String name)
    throws RocaroException;
}
