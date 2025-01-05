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

import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.services.RCServiceFrameScopedType;

/**
 * The contextual information provided to executing operations.
 */

public interface RCGOperationExecutionContextType
{
  /**
   * @return The current frame information
   */

  RCFrameInformation frameInformation();

  /**
   * Obtain a reference to a frame-scoped service.
   *
   * @param serviceClass The service class
   * @param <T>          The type of service
   *
   * @return A frame-scoped service
   */

  <T extends RCServiceFrameScopedType> T frameScopedService(
    Class<T> serviceClass);

  /**
   * Supply a resource value to the given port.
   *
   * @param port     The port
   * @param resource The resource
   * @param <R>      The type of resource
   */

  <R extends RCResourceType>
  void portWrite(
    RCGPortSourceType<R> port,
    R resource
  );

  /**
   * Obtain the current value of the port.
   *
   * @param port         The port
   * @param resourceType The resource type
   * @param <R>          The type of resource
   *
   * @return The current value
   */

  <R extends RCResourceType> R portRead(
    RCGPortTargetType<R> port,
    Class<R> resourceType
  );
}
