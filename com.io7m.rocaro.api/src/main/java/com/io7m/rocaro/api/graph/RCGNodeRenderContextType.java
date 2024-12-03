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

/**
 * The context provided to each node in a render graph during the
 * rendering of a frame.
 */

public interface RCGNodeRenderContextType
  extends RCGNodeContextType
{
  /**
   * Write a value to the given port.
   *
   * @param port  The port
   * @param value The value
   * @param <T>   The type of values
   */

  <T> void portWrite(
    RCGPortProducer<T> port,
    T value);

  /**
   * @param port The port
   * @param <T>  The type of port values
   *
   * @return {@code true} if the port has been written
   */

  <T> boolean portIsWritten(
    RCGPortProducer<T> port);

  /**
   * Read a value that was previously written to the given port.
   *
   * @param port The port
   * @param <T>  The type of values
   *
   * @return The value
   */

  <T> T portWritten(
    RCGPortProducer<T> port);

  /**
   * Read a value from the given port.
   *
   * @param port The port
   * @param <T>  The type of values
   *
   * @return The value written by the producer connected to the given port
   */

  <T> T portRead(
    RCGPortConsumer<T> port);

  /**
   * Read a value from the given port.
   *
   * @param port The port
   * @param <T>  The type of values
   *
   * @return The value written by the producer connected to the given port
   */

  <T> T portRead(
    RCGPortModifier<T> port);
}
