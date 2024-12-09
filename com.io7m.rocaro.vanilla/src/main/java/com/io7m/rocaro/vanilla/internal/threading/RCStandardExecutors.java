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


package com.io7m.rocaro.vanilla.internal.threading;

import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCStrings;

import java.util.Objects;

import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.GPU;
import static com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel.MAIN;

/**
 * The standard set of executors used by the renderer.
 */

public final class RCStandardExecutors
  implements RCCloseableType
{
  private final RCExecutorType mainExecutor;
  private final RCExecutorType gpuExecutor;
  private final CloseableCollectionType<RocaroException> resources;

  private RCStandardExecutors(
    final RCStrings strings,
    final RCExecutorType inGpuExecutor,
    final RCExecutorType inMainExecutor)
  {
    this.gpuExecutor =
      Objects.requireNonNull(inGpuExecutor, "gpuExecutor");
    this.mainExecutor =
      Objects.requireNonNull(inMainExecutor, "mainExecutor");

    this.resources = RCResourceCollections.create(strings);
    this.resources.add(this.mainExecutor);
    this.resources.add(this.gpuExecutor);
  }

  /**
   * @return The executor for GPU tasks
   */

  public RCExecutorType gpuExecutor()
  {
    return this.gpuExecutor;
  }

  /**
   * @return The main executor for coordinating tasks
   */

  public RCExecutorType mainExecutor()
  {
    return this.mainExecutor;
  }

  /**
   * Create the standard executors.
   *
   * @param strings    The string resources
   * @param rendererId The renderer ID
   *
   * @return The standard executors
   */

  public static RCStandardExecutors create(
    final RCStrings strings,
    final RCRendererID rendererId)
  {
    return new RCStandardExecutors(
      strings,
      RCExecutorOne.create(rendererId, "gpu", GPU),
      RCExecutorOne.create(rendererId, "main", MAIN)
    );
  }

  @Override
  public void close()
    throws RocaroException
  {
    this.resources.close();
  }
}
