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


package com.io7m.rocaro.vanilla.internal.fences;

import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceException;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCServiceException;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.frames.RCFrameServiceType;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutors;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static com.io7m.jcoronado.api.VulkanLogicalDeviceType.VulkanFenceStatus.VK_FENCE_SIGNALLED;
import static com.io7m.rocaro.api.RCUnit.UNIT;

/**
 * The fence service.
 */

public final class RCFenceService
  extends RCObject
  implements RCFenceServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCFenceService.class);

  private final CloseableCollectionType<RocaroException> resources;
  private final RCDeviceType device;
  private final ConcurrentHashMap.KeySetView<Fence, Boolean> fencesGraphics;
  private final ConcurrentHashMap.KeySetView<Fence, Boolean> fencesCompute;
  private final ConcurrentHashMap.KeySetView<Fence, Boolean> fencesTransfer;

  private RCFenceService(
    final CloseableCollectionType<RocaroException> inResources,
    final RCVulkanRendererType inVulkan)
  {
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.device =
      inVulkan.device();

    this.fencesGraphics =
      ConcurrentHashMap.newKeySet();
    this.fencesCompute =
      ConcurrentHashMap.newKeySet();
    this.fencesTransfer =
      ConcurrentHashMap.newKeySet();
  }

  /**
   * Create a fence service.
   *
   * @param services The service directory
   *
   * @return The service
   *
   * @throws RCServiceException On errors
   */

  public static RCFenceService create(
    final RPServiceDirectoryType services)
    throws RCServiceException
  {
    try {
      final var vulkan =
        services.requireService(RCVulkanRendererType.class);
      final var frame =
        services.requireService(RCFrameServiceType.class);
      final var strings =
        services.requireService(RCStrings.class);
      final var resources =
        RCResourceCollections.create(strings);

      final var fences =
        new RCFenceService(resources, vulkan);

      final var frameInfo =
        frame.frameInformation();

      resources.add(frameInfo.subscribe((_, _) -> fences.checkGraphicsFences()));
      resources.add(frameInfo.subscribe((_, _) -> fences.checkTransferFences()));
      resources.add(frameInfo.subscribe((_, _) -> fences.checkComputeFences()));
      return fences;
    } catch (final RPServiceException | IllegalStateException e) {
      throw new RCServiceException(e);
    }
  }

  private void checkComputeFences()
  {
    this.checkFences(this.fencesCompute);
  }

  private void checkTransferFences()
  {
    this.checkFences(this.fencesTransfer);
  }

  private void checkGraphicsFences()
  {
    this.checkFences(this.fencesGraphics);
  }

  private void checkFences(
    final Set<Fence> fences)
  {
    final var remove = new HashSet<Fence>();
    for (final var fence : fences) {
      try {
        if (fence.isSignalled()) {
          remove.add(fence);
          fence.future.complete(UNIT);
        }
      } catch (final RocaroException e) {
        remove.add(fence);
        fence.future.completeExceptionally(e);
      }
    }

    for (final var fence : remove) {
      fences.remove(fence);
    }
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
    this.resources.close();
  }

  @Override
  public String description()
  {
    return "Fence notification service.";
  }

  @Override
  public CompletableFuture<?> registerGraphicsFence(
    final VulkanFenceType fence)
  {
    Objects.requireNonNull(fence, "fence");

    final var future = new CompletableFuture<RCUnit>();
    final var exec = this.device.graphicsExecutor();
    this.fencesGraphics.add(new Fence(this.device, exec, future, fence));
    return future;
  }

  @Override
  public CompletableFuture<?> registerComputeFence(
    final VulkanFenceType fence)
  {
    Objects.requireNonNull(fence, "fence");

    final var future = new CompletableFuture<RCUnit>();
    final var exec = this.device.computeExecutor();
    this.fencesCompute.add(new Fence(this.device, exec, future, fence));
    return future;
  }

  @Override
  public CompletableFuture<?> registerTransferFence(
    final VulkanFenceType fence)
  {
    Objects.requireNonNull(fence, "fence");

    final var future = new CompletableFuture<RCUnit>();
    final var exec = this.device.transferExecutor();
    this.fencesTransfer.add(new Fence(this.device, exec, future, fence));
    return future;
  }

  private static final class Fence
  {
    private final CompletableFuture<RCUnit> future;
    private final VulkanFenceType fence;
    private final RCDeviceType device;
    private final ExecutorService executor;

    Fence(
      final RCDeviceType inDevice,
      final ExecutorService inExecutor,
      final CompletableFuture<RCUnit> inFuture,
      final VulkanFenceType inFence)
    {
      this.device =
        Objects.requireNonNull(inDevice, "device");
      this.executor =
        Objects.requireNonNull(inExecutor, "executor");
      this.future =
        Objects.requireNonNull(inFuture, "future");
      this.fence =
        Objects.requireNonNull(inFence, "fence");
    }

    boolean isSignalled()
      throws RocaroException
    {
      return RCExecutors.executeAndWait(this.executor, () -> {
        final var status =
          this.device.device().getFenceStatus(this.fence);
        return status == VK_FENCE_SIGNALLED;
      });
    }
  }
}
