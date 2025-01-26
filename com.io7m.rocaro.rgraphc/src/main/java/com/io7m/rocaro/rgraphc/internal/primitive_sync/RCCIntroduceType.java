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


package com.io7m.rocaro.rgraphc.internal.primitive_sync;

import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPPlaceholderType;
import com.io7m.rocaro.rgraphc.internal.primitive_graph.RCCPortPrimitiveProducer;
import com.io7m.rocaro.rgraphc.internal.typed.RCTOperationDeclaration;

/**
 * The type of commands that introduce resources.
 */

public sealed interface RCCIntroduceType
  extends RCCMetaCommandType
  permits RCCIntroduceImage, RCCIntroduceMemory
{
  /**
   * @return The operation that owns this command
   */

  @Override
  default RCTOperationDeclaration operation()
  {
    return this.owner().operation();
  }

  /**
   * @return The port at which the resource will be introduced
   */

  RCCPortPrimitiveProducer port();

  /**
   * @return The execution command that introduces the resource
   */

  RCCExecute owner();

  /**
   * @return The resource
   */

  RCCPPlaceholderType resource();
}
