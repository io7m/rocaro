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
 * The pipeline stages supported by commands.
 */

public enum RCGCommandPipelineStage
{
  /**
   * The stage that accesses indirect draw commands.
   */

  STAGE_RENDER_DRAW_INDIRECT,

  /**
   * The stage that executes vertex shaders.
   */

  STAGE_RENDER_VERTEX_SHADER,

  /**
   * The stage that performs early fragment tests such as depth and stencil
   * tests.
   */

  STAGE_RENDER_FRAGMENT_SHADER_EARLY_TESTS,

  /**
   * The stage that executes fragment shaders.
   */

  STAGE_RENDER_FRAGMENT_SHADER,

  /**
   * The stage that executes late fragment tests for fragment shaders.
   */

  STAGE_RENDER_FRAGMENT_SHADER_LATE_TESTS,

  /**
   * The stage of the pipeline where final color values are output from
   * the pipeline.
   */

  STAGE_RENDER_COLOR_ATTACHMENT_OUTPUT,

  /**
   * The stage that executes compute shaders.
   */

  STAGE_COMPUTE_SHADER,

  /**
   * The stage representing execution on the CPU.
   */

  STAGE_CPU,

  /**
   * The stage that executes copy commands.
   */

  STAGE_TRANSFER_COPY,

  /**
   * The stage that executes image blit commands.
   */

  STAGE_TRANSFER_BLIT,

  /**
   * The stage that executes image resolution commands.
   */

  STAGE_TRANSFER_RESOLVE,

  /**
   * The stage that executes image clear commands.
   */

  STAGE_TRANSFER_CLEAR
}
