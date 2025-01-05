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

import com.io7m.rocaro.api.resources.RCResourceSchematicType;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.resources.RCSchematicConstraintType;

import java.util.Set;

/**
 * The context passed to operations on creation.
 */

public interface RCGOperationCreationContextType
{
  /**
   * Create a producer port.
   *
   * @param owner          The operation that owns the port
   * @param name           The port name
   * @param readsAtStages  The stages at which the resource will be read
   * @param typeConstraint The resource type constraint
   * @param writesAtStages The stages at which the resource will be written
   * @param <R>            The type of resource
   * @param <S>            The type of resource schematic
   *
   * @return The port
   */

  <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortProducerType<R> createProducerPort(
    RCGOperationType owner,
    RCGPortName name,
    Set<RCGCommandPipelineStage> readsAtStages,
    RCSchematicConstraintType<S> typeConstraint,
    Set<RCGCommandPipelineStage> writesAtStages
  );

  /**
   * Create a modifier port.
   *
   * @param owner          The operation that owns the port
   * @param name           The port name
   * @param readsAtStages  The stages at which the resource will be read
   * @param typeConsumes   The consumed resource type constraint
   * @param writesAtStages The stages at which the resource will be written
   * @param typeProduces   The produced resource type constraint
   * @param <R>            The type of resource
   * @param <S>            The type of resource schematic
   *
   * @return The port
   */

  <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortModifierType<R> createModifierPort(
    RCGOperationType owner,
    RCGPortName name,
    Set<RCGCommandPipelineStage> readsAtStages,
    RCSchematicConstraintType<S> typeConsumes,
    Set<RCGCommandPipelineStage> writesAtStages,
    RCSchematicConstraintType<S> typeProduces
  );

  /**
   * Create a consumer port.
   *
   * @param owner          The operation that owns the port
   * @param name           The port name
   * @param readsAtStages  The stages at which the resource will be read
   * @param typeConstraint The resource type constraint
   * @param writesAtStages The stages at which the resource will be written
   * @param <R>            The type of resource
   * @param <S>            The type of resource schematic
   *
   * @return The port
   */

  <R extends RCResourceType, S extends RCResourceSchematicType>
  RCGPortConsumerType<R> createConsumerPort(
    RCGOperationType owner,
    RCGPortName name,
    Set<RCGCommandPipelineStage> readsAtStages,
    RCSchematicConstraintType<S> typeConstraint,
    Set<RCGCommandPipelineStage> writesAtStages
  );
}
