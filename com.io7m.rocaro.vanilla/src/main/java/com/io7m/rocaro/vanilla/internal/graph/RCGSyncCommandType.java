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


package com.io7m.rocaro.vanilla.internal.graph;

import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.api.graph.RCGCommandPipelineStage;
import com.io7m.rocaro.api.graph.RCGOperationType;
import com.io7m.rocaro.api.graph.RCGResourceImageLayout;
import com.io7m.rocaro.api.graph.RCGResourcePlaceholderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public sealed interface RCGSyncCommandType
{
  Submission submission();

  sealed interface WriteType extends RCGSyncCommandType {
    Execute owner();

    RCGCommandPipelineStage writeStage();
  }

  sealed interface ReadType extends RCGSyncCommandType {
    Execute owner();
  }

  record Write(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage writesAt)
    implements WriteType
  {
    public Write
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(writesAt, "writesAt");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }

    @Override
    public RCGCommandPipelineStage writeStage()
    {
      return this.writesAt;
    }
  }

  record Read(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage readsAt)
    implements ReadType
  {
    public Read
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(readsAt, "readsAt");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }
  }

  record MemoryReadBarrier(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage waitsForWriteAt,
    RCGCommandPipelineStage blocksReadAt)
    implements ReadType
  {
    public MemoryReadBarrier
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(waitsForWriteAt, "waitsForWriteAt");
      Objects.requireNonNull(blocksReadAt, "blocksReadAt");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }
  }

  record MemoryWriteBarrier(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage waitsForWriteAt,
    RCGCommandPipelineStage blocksWriteAt)
    implements WriteType
  {
    public MemoryWriteBarrier
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(waitsForWriteAt, "waitsForWriteAt");
      Objects.requireNonNull(blocksWriteAt, "blocksWriteAt");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }

    @Override
    public RCGCommandPipelineStage writeStage()
    {
      return this.blocksWriteAt;
    }
  }

  record ImageReadBarrier(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage waitsForWriteAt,
    RCGCommandPipelineStage blocksReadAt,
    RCGResourceImageLayout layoutFrom,
    RCGResourceImageLayout layoutTo)
    implements ReadType
  {
    public ImageReadBarrier
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(waitsForWriteAt, "waitsForWriteAt");
      Objects.requireNonNull(blocksReadAt, "blocksReadAt");
      Objects.requireNonNull(layoutFrom, "layoutFrom");
      Objects.requireNonNull(layoutTo, "layoutTo");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }
  }

  record ImageWriteBarrier(
    Execute owner,
    RCGResourcePlaceholderType resource,
    RCGCommandPipelineStage waitsForWriteAt,
    RCGCommandPipelineStage blocksWriteAt,
    RCGResourceImageLayout layoutFrom,
    RCGResourceImageLayout layoutTo)
    implements WriteType
  {
    public ImageWriteBarrier
    {
      Objects.requireNonNull(owner, "owner");
      Objects.requireNonNull(resource, "resource");
      Objects.requireNonNull(waitsForWriteAt, "waitsForWriteAt");
      Objects.requireNonNull(blocksWriteAt, "blocksWriteAt");
      Objects.requireNonNull(layoutFrom, "layoutFrom");
      Objects.requireNonNull(layoutTo, "layoutTo");
    }

    public RCGOperationType operation()
    {
      return this.owner.operation();
    }

    @Override
    public Submission submission()
    {
      return this.owner.submission();
    }

    @Override
    public RCGCommandPipelineStage writeStage()
    {
      return this.blocksWriteAt;
    }
  }

  final class Submission
    implements RCGSyncCommandType
  {
    private final ArrayList<Execute> operations;
    private final RCDeviceQueueCategory queue;
    private final int id;

    Submission(
      final RCDeviceQueueCategory queue,
      final int id)
    {
      this.queue = queue;
      this.id = id;
      this.operations = new ArrayList<>();
    }

    public void addOperation(
      final Execute operation)
    {
      this.operations.add(
        Objects.requireNonNull(operation, "operation")
      );
    }

    @Override
    public Submission submission()
    {
      return this;
    }

    public RCDeviceQueueCategory queue()
    {
      return this.queue;
    }

    public int id()
    {
      return this.id;
    }
  }

  final class Execute
    implements RCGSyncCommandType
  {
    private final Set<Read> reads;
    private final Set<Write> writes;
    private final Submission submission;
    private final RCGOperationType operation;
    private final Set<Read> readsR;
    private final Set<Write> writesR;

    Execute(
      final Submission submission,
      final RCGOperationType operation)
    {
      this.submission =
        Objects.requireNonNull(submission, "submission");
      this.operation =
        Objects.requireNonNull(operation, "operation");
      this.reads =
        new HashSet<>();
      this.writes =
        new HashSet<>();
      this.readsR =
        Collections.unmodifiableSet(this.reads);
      this.writesR =
        Collections.unmodifiableSet(this.writes);
    }

    @Override
    public String toString()
    {
      return "[Execute %s]".formatted(this.operation.name());
    }

    public RCGOperationType operation()
    {
      return this.operation;
    }

    public Set<Read> reads()
    {
      return this.readsR;
    }

    public Set<Write> writes()
    {
      return this.writesR;
    }

    public Read addRead(
      final RCGResourcePlaceholderType resource,
      final RCGCommandPipelineStage stage)
    {
      final var r = new Read(this, resource, stage);
      this.reads.add(r);
      return r;
    }

    public Write addWrite(
      final RCGResourcePlaceholderType resource,
      final RCGCommandPipelineStage stage)
    {
      final var w = new Write(this, resource, stage);
      this.writes.add(w);
      return w;
    }

    public Optional<Write> findWriteOn(
      final RCGResourcePlaceholderType resource,
      final RCGCommandPipelineStage stage)
    {
      return this.writesR.stream()
        .filter(w -> Objects.equals(w.resource, resource) && w.writesAt == stage)
        .findFirst();
    }

    public Submission submission()
    {
      return this.submission;
    }
  }
}
