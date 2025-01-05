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


package com.io7m.rocaro.vanilla.internal.graph_exec;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jcoronado.api.VulkanBufferMemoryBarrier;
import com.io7m.jcoronado.api.VulkanCommandBufferSubmitInfo;
import com.io7m.jcoronado.api.VulkanCommandBufferType;
import com.io7m.jcoronado.api.VulkanDebuggingType;
import com.io7m.jcoronado.api.VulkanDependencyInfo;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanImageMemoryBarrier;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanSubmitInfo;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RendererGraphProcedureType;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.buffers.RCBufferType;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.api.graph.RCGBarrierBufferType;
import com.io7m.rocaro.api.graph.RCGBarrierImageType;
import com.io7m.rocaro.api.graph.RCGExecuteOperation;
import com.io7m.rocaro.api.graph.RCGExecutionBarrierSet;
import com.io7m.rocaro.api.graph.RCGExecutionItemType;
import com.io7m.rocaro.api.graph.RCGExecutionSubmissionType;
import com.io7m.rocaro.api.graph.RCGGraphType;
import com.io7m.rocaro.api.graph.RCGOperationExecutionContextType;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortTargetType;
import com.io7m.rocaro.api.graph.RCGQueueTransferType;
import com.io7m.rocaro.api.graph.RCGResourceVariable;
import com.io7m.rocaro.api.images.RCImage2DType;
import com.io7m.rocaro.api.resources.RCResourceType;
import com.io7m.rocaro.api.services.RCServiceFrameScopedType;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanFrameType;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.jcoronado.api.VulkanCommandBufferLevel.VK_COMMAND_BUFFER_LEVEL_PRIMARY;

public final class RCGraphFrameExecutor implements
  RCGOperationExecutionContextType
{
  private final RPServiceDirectory services;
  private final RCGraphExecutor graphExecutor;
  private final RCGGraphType graph;
  private final RCFrameIndex frameIndex;
  private final HashMap<RCGResourceVariable<?>, RCResourceType> portResourcesWritten;
  private RCDeviceType device;
  private VulkanLogicalDeviceType logicalDevice;
  private VulkanDebuggingType debugging;
  private RCFrameInformation frameInformation;

  RCGraphFrameExecutor(
    final RCGraphExecutor inGraphExecutor,
    final RCGGraphType inGraph,
    final RCFrameIndex inFrameIndex)
  {
    this.graphExecutor =
      Objects.requireNonNull(inGraphExecutor, "graphExecutor");
    this.graph =
      Objects.requireNonNull(inGraph, "graph");
    this.frameIndex =
      Objects.requireNonNull(inFrameIndex, "inFrameIndex");
    this.services =
      new RPServiceDirectory();
    this.portResourcesWritten =
      new HashMap<>();
  }

  public void executeFrame(
    final RCFrameInformation newFrameInformation,
    final RCVulkanFrameType frame,
    final RendererGraphProcedureType f)
    throws RocaroException
  {
    Objects.requireNonNull(newFrameInformation, "frameInformation");
    Objects.requireNonNull(frame, "frame");
    Objects.requireNonNull(f, "f");

    Preconditions.checkPreconditionV(
      newFrameInformation.frameIndex(),
      Objects.equals(newFrameInformation.frameIndex(), this.frameIndex),
      "Frame index must match %s",
      this.frameIndex
    );

    this.frameInformation = newFrameInformation;
    this.services.register(RCVulkanFrameType.class, frame);

    try {
      this.portResourcesWritten.clear();

      this.device =
        frame.device();
      this.logicalDevice =
        this.device.device();
      this.debugging =
        this.logicalDevice.debugging();

      final var plan =
        this.graph.executionPlan();

      for (final var submission : plan.submissions()) {
        final var submissionBuffer =
          this.logicalDevice.createCommandBuffer(
            frame.commandPool(),
            VK_COMMAND_BUFFER_LEVEL_PRIMARY
          );

        this.debugging.setObjectName(
          submissionBuffer,
          "Frame %s (%s)".formatted(
            newFrameInformation.frameNumber().value(),
            submission.submissionId()
          )
        );

        this.executeSubmissionItems(submissionBuffer, submission);

        final var queue =
          submission.submissionId()
            .queue();

        final var submissionInfo =
          VulkanSubmitInfo.builder()
            .addCommandBuffers(
              VulkanCommandBufferSubmitInfo.builder()
                .setCommandBuffer(submissionBuffer)
                .build()
            ).build();

        this.device.submit(
          queue,
          List.of(submissionInfo),
          Optional.empty()
        );
      }
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private void executeSubmissionItems(
    final VulkanCommandBufferType submissionBuffer,
    final RCGExecutionSubmissionType submission)
    throws VulkanException, RocaroException
  {
    for (final var transfer : submission.startingQueueTransfers()) {
      this.executeStartingTransfer(submissionBuffer, submission, transfer);
    }

    for (final var item : submission.items()) {
      this.executeItem(submissionBuffer, item);
    }

    for (final var transfer : submission.startingQueueTransfers()) {
      this.executeEndingTransfer(submissionBuffer, submission, transfer);
    }
  }

  private void executeStartingTransfer(
    final VulkanCommandBufferType submissionBuffer,
    final RCGExecutionSubmissionType submission,
    final RCGQueueTransferType transfer)
  {
    Invariants.checkInvariantV(
      transfer.queueTarget() == submission.submissionId(),
      "Transfer target must match this submission."
    );
  }

  private void executeEndingTransfer(
    final VulkanCommandBufferType submissionBuffer,
    final RCGExecutionSubmissionType submission,
    final RCGQueueTransferType transfer)
  {
    Invariants.checkInvariantV(
      transfer.queueSource() == submission.submissionId(),
      "Transfer source must match this submission."
    );
  }

  private void executeItem(
    final VulkanCommandBufferType commandBuffer,
    final RCGExecutionItemType item)
    throws VulkanException, RocaroException
  {
    switch (item) {
      case final RCGExecutionBarrierSet barrier -> {
        this.executeBarrierSet(commandBuffer, barrier);
      }
      case final RCGExecuteOperation operation -> {
        this.executeOperation(commandBuffer, operation);
      }
    }
  }

  private void executeOperation(
    final VulkanCommandBufferType commandBuffer,
    final RCGExecuteOperation operation)
    throws VulkanException, RocaroException
  {
    try (final var r =
           this.debugging.begin(commandBuffer, operation.name())) {
      operation.operation().execute(this);
    }
  }

  private void executeBarrierSet(
    final VulkanCommandBufferType commandBuffer,
    final RCGExecutionBarrierSet barrierSet)
    throws VulkanException
  {
    final var builder =
      VulkanDependencyInfo.builder();

    for (final var barrier : barrierSet.barriers()) {
      switch (barrier) {
        case final RCGBarrierBufferType b -> {
          builder.addBufferMemoryBarriers(this.makeBufferMemoryBarrier(b));
        }
        case final RCGBarrierImageType b -> {
          builder.addImageMemoryBarriers(this.makeImageMemoryBarrier(b));
        }
      }
    }

    commandBuffer.pipelineBarrier(builder.build());
  }

  private VulkanImageMemoryBarrier makeImageMemoryBarrier(
    final RCGBarrierImageType barrier)
  {
    final var image =
      (RCImage2DType) this.portResourcesWritten.get(barrier.image());

    throw new UnimplementedCodeException();
  }

  private VulkanBufferMemoryBarrier makeBufferMemoryBarrier(
    final RCGBarrierBufferType barrier)
  {
    final var buffer =
      (RCBufferType) this.portResourcesWritten.get(barrier.buffer());

    throw new UnimplementedCodeException();
  }

  @Override
  public RCFrameInformation frameInformation()
  {
    return this.frameInformation;
  }

  @Override
  public <T extends RCServiceFrameScopedType> T frameScopedService(
    final Class<T> serviceClass)
  {
    return this.services.requireService(serviceClass);
  }

  @Override
  public <R extends RCResourceType> void portWrite(
    final RCGPortSourceType<R> port,
    final R resource)
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(resource, "resource");

    final var resourceSymbol =
      this.graph.resourceAt(port);

    Objects.requireNonNull(resourceSymbol, "resourceSymbol");
    this.portResourcesWritten.put(resourceSymbol, resource);
  }

  @Override
  public <R extends RCResourceType> R portRead(
    final RCGPortTargetType<R> port,
    final Class<R> resourceType)
  {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(resourceType, "resourceType");

    final var resourceSymbol =
      this.graph.resourceAt(port);
    Objects.requireNonNull(resourceSymbol, "resourceSymbol");

    final var resource =
      this.portResourcesWritten.get(resourceSymbol);
    return resourceType.cast(resource);
  }
}
