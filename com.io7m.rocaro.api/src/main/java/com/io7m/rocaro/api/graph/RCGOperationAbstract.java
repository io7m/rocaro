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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.graph.RCGOperationStatusType.PreparationFailed;
import com.io7m.rocaro.api.graph.RCGOperationStatusType.Preparing;
import com.io7m.rocaro.api.graph.RCGOperationStatusType.Ready;
import com.io7m.rocaro.api.graph.RCGOperationStatusType.Uninitialized;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.rocaro.api.graph.RCGOperationStatusType.Uninitialized.UNINITIALIZED;

/**
 * An abstract implementation of an operation.
 */

public abstract class RCGOperationAbstract
  extends RCObject
  implements RCGOperationType
{
  private final RCGOperationName name;
  private final TreeMap<RCGPortName, RCGPortType> portsM;
  private final Map<RCGPortName, RCGPortType> ports;
  private volatile RCGOperationStatusType status;
  private final RCDeviceQueueCategory queueCategory;

  protected RCGOperationAbstract(
    final RCGOperationName inName,
    final RCDeviceQueueCategory inQueueCategory)
  {
    this.name =
      Objects.requireNonNull(inName, "name");
    this.queueCategory =
      Objects.requireNonNull(inQueueCategory, "inQueueCategory");
    this.status =
      UNINITIALIZED;
    this.portsM =
      new TreeMap<>();
    this.ports =
      Collections.unmodifiableSortedMap(this.portsM);
  }

  protected final <P extends RCGPortType> P addPort(
    final P port)
  {
    Preconditions.checkPreconditionV(
      port.owner() == this,
      "Port must be owned by this operation."
    );
    Preconditions.checkPreconditionV(
      !this.portsM.containsKey(port.name()),
      "Port names must be unique within an operation."
    );

    this.portsM.put(port.name(), port);
    return port;
  }

  @Override
  public final Map<RCGPortName, RCGPortType> ports()
  {
    return this.ports;
  }

  protected final void setStatus(
    final RCGOperationStatusType newStatus)
  {
    this.status = Objects.requireNonNull(newStatus, "newStatus");
  }

  protected abstract void onExecute(
    RCGOperationExecutionContextType context)
    throws RocaroException;

  @Override
  public final void execute(
    final RCGOperationExecutionContextType context)
    throws RocaroException
  {
    Objects.requireNonNull(context, "context");

    switch (this.status) {
      case final PreparationFailed s -> throw s.exception();
      case final Preparing s -> throw this.errorOperationPreparing(s);
      case final Ready _ -> this.onExecute(context);
      case final Uninitialized s -> throw this.errorOperationNotPrepared(s);
    }
  }

  private RocaroException errorOperationNotPrepared(
    final Uninitialized statusNow)
  {
    return new RCGGraphException(
      "Operation has not been prepared.",
      Map.ofEntries(
        Map.entry("Operation", this.name.value()),
        Map.entry("Status", statusNow.toString())
      ),
      "error-operation-preparing",
      Optional.of(
        "Call prepare() before trying to call record() or execute()."
      )
    );
  }

  private RCGGraphException errorOperationPreparing(
    final Preparing preparing)
  {
    return new RCGGraphException(
      "Operation is still preparing.",
      Map.ofEntries(
        Map.entry("Operation", this.name.value()),
        Map.entry("Status", preparing.message()),
        Map.entry("Progress", Double.toString(preparing.progress()))
      ),
      "error-operation-preparing",
      Optional.empty()
    );
  }

  protected abstract void onPrepare(
    RCGOperationPreparationContextType context)
    throws RocaroException;

  protected abstract void onPrepareCheck(
    RCGOperationPreparationContextType context)
    throws RocaroException;

  @Override
  public final void prepare(
    final RCGOperationPreparationContextType context)
    throws RocaroException
  {
    Objects.requireNonNull(context, "context");

    switch (this.status) {
      case final PreparationFailed _ -> this.onPrepare(context);
      case final Preparing _ -> this.onPrepareCheck(context);
      case final Ready _ -> {
        // Nothing to do.
      }
      case final Uninitialized _ -> this.onPrepare(context);
    }
  }

  @Override
  public final RCGOperationName name()
  {
    return this.name;
  }

  @Override
  public final RCGOperationStatusType status()
  {
    return this.status;
  }

  @Override
  public final RCDeviceQueueCategory queueCategory()
  {
    return this.queueCategory;
  }
}
