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

import com.io7m.rocaro.api.graph.RCGGraphStatusType;
import com.io7m.rocaro.api.graph.RCGraphName;

import java.util.Objects;

/**
 * A frame builder.
 */

public interface RendererFrameBuilderType
{
  /**
   * @return Information about the current frame
   */

  RCFrameInformation frameInformation();

  /**
   * Prepare the given graph.
   *
   * @param graphName The graph name
   *
   * @throws RocaroException On errors
   */

  default void prepare(
    final String graphName)
    throws RocaroException
  {
    this.prepare(new RCGraphName(graphName));
  }

  /**
   * Prepare the given graph.
   *
   * @param graphName The graph name
   *
   * @throws RocaroException On errors
   */

  void prepare(
    RCGraphName graphName)
    throws RocaroException;

  /**
   * @param graphName The graph name
   *
   * @return The status of the given graph
   *
   * @throws RocaroException On errors
   */

  default RCGGraphStatusType graphStatus(
    final String graphName)
    throws RocaroException
  {
    return this.graphStatus(new RCGraphName(graphName));
  }

  /**
   * @param graphName The graph name
   *
   * @return The status of the given graph
   *
   * @throws RocaroException On errors
   */

  RCGGraphStatusType graphStatus(
    RCGraphName graphName)
    throws RocaroException;

  /**
   * Execute the given graph.
   *
   * @param graphName The graph name
   * @param f         The frame function
   *
   * @throws RocaroException On errors
   */

  void executeGraph(
    RCGraphName graphName,
    RendererGraphProcedureType f)
    throws RocaroException;

  /**
   * Execute the given graph.
   *
   * @param graphName The graph name
   * @param f         The frame function
   *
   * @throws RocaroException On errors
   */

  default void executeGraph(
    final String graphName,
    final RendererGraphProcedureType f)
    throws RocaroException
  {
    Objects.requireNonNull(graphName, "graphName");
    Objects.requireNonNull(f, "f");

    this.executeGraph(new RCGraphName(graphName), f);
  }

  /**
   * Execute the given graph.
   *
   * @param graphName The graph name
   *
   * @throws RocaroException On errors
   */

  default void executeGraph(
    final RCGraphName graphName)
    throws RocaroException
  {
    this.executeGraph(graphName, _ -> {

    });
  }

  /**
   * Execute the given graph.
   *
   * @param graphName The graph name
   *
   * @throws RocaroException On errors
   */

  default void executeGraph(
    final String graphName)
    throws RocaroException
  {
    this.executeGraph(new RCGraphName(graphName), _ -> {

    });
  }
}
