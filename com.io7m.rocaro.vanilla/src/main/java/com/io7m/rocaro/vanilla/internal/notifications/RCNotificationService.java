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


package com.io7m.rocaro.vanilla.internal.notifications;

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanFenceType;
import com.io7m.jcoronado.api.VulkanSemaphoreTimelineWait;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceException;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.devices.RCDeviceType;
import com.io7m.rocaro.vanilla.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCResourceCollections;
import com.io7m.rocaro.vanilla.internal.RCServiceException;
import com.io7m.rocaro.vanilla.internal.threading.RCExecutorOne;
import com.io7m.rocaro.vanilla.internal.threading.RCThreadLabel;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanRendererType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.jcoronado.api.VulkanLogicalDeviceType.VulkanFenceStatus.VK_FENCE_SIGNALLED;
import static com.io7m.jcoronado.api.VulkanLogicalDeviceType.VulkanWaitStatus.VK_WAIT_SUCCEEDED;

/**
 * The notification service.
 */

public final class RCNotificationService
  extends RCObject
  implements RCNotificationServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCNotificationService.class);

  private final CloseableCollectionType<RocaroException> resources;
  private final RCDeviceType device;
  private final ConcurrentHashMap.KeySetView<Fence, Boolean> fences;
  private final ConcurrentHashMap.KeySetView<TimelineSemaphore, Boolean> timelineSemaphores;
  private final AtomicBoolean closed;
  private final Duration checkFrequency;
  private final RCRendererID rendererId;

  private RCNotificationService(
    final CloseableCollectionType<RocaroException> inResources,
    final RCVulkanRendererType inVulkan,
    final Duration inCheckFrequency,
    final RCRendererID inRendererId)
  {
    this.checkFrequency =
      Objects.requireNonNull(inCheckFrequency, "checkFrequency");
    this.rendererId =
      Objects.requireNonNull(inRendererId, "rendererId");
    this.closed =
      new AtomicBoolean(false);
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.device =
      inVulkan.device();
    this.fences =
      ConcurrentHashMap.newKeySet();
    this.timelineSemaphores =
      ConcurrentHashMap.newKeySet();
  }

  /**
   * Create a notification service.
   *
   * @param services       The service directory
   * @param rendererId     The renderer ID
   * @param checkFrequency The frequency at which to check for notifications
   *
   * @return The service
   *
   * @throws RCServiceException On errors
   */

  public static RCNotificationService create(
    final RPServiceDirectoryType services,
    final RCRendererID rendererId,
    final Duration checkFrequency)
    throws RCServiceException
  {
    try {
      final var vulkan =
        services.requireService(RCVulkanRendererType.class);
      final var strings =
        services.requireService(RCStrings.class);
      final var resources =
        RCResourceCollections.create(strings);
      final var executor =
        resources.add(
          RCExecutorOne.create(
            vulkan.id(),
            "notifications",
            RCThreadLabel.UNSPECIFIED
          )
        );

      final var service =
        new RCNotificationService(
          resources,
          vulkan,
          checkFrequency,
          rendererId
        );

      executor.execute(service::run);
      return service;
    } catch (final RPServiceException | IllegalStateException e) {
      throw new RCServiceException(e);
    }
  }

  private void run()
  {
    while (!this.closed.get()) {
      try {
        this.runOne();
      } catch (final Throwable e) {
        LOG.debug("Notification exception: ", e);
      }
    }
  }

  private void runOne()
  {
    final var fencesNow =
      List.copyOf(this.fences);
    for (final var fence : fencesNow) {
      this.runCheckFence(fence);
    }

    final var semaphoresNow =
      List.copyOf(this.timelineSemaphores);
    for (final var semaphore : semaphoresNow) {
      this.runCheckSemaphore(semaphore);
    }

    try {
      Thread.sleep(this.checkFrequency);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void runCheckSemaphore(
    final TimelineSemaphore semaphore)
  {
    try {
      if (semaphore.isSignalled()) {
        this.timelineSemaphores.remove(semaphore);
        semaphore.future.complete(RCUnit.UNIT);
      }
    } catch (final RocaroException e) {
      this.timelineSemaphores.remove(semaphore);
      semaphore.future.completeExceptionally(e);
    }
  }

  private void runCheckFence(
    final Fence fence)
  {
    try {
      if (fence.isSignalled()) {
        this.fences.remove(fence);
        fence.future.complete(RCUnit.UNIT);
      }
    } catch (final RocaroException e) {
      this.fences.remove(fence);
      fence.future.completeExceptionally(e);
    }
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
    this.closed.set(true);
    this.resources.close();
  }

  @Override
  public String description()
  {
    return "Notification service.";
  }

  @Override
  public CompletableFuture<?> registerFence(
    final VulkanFenceType fence)
  {
    final var future = new CompletableFuture<RCUnit>();
    this.fences.add(new Fence(this.device, future, fence));
    return future;
  }

  @Override
  public CompletableFuture<?> registerTimelineSemaphore(
    final VulkanSemaphoreTimelineWait semaphore)
  {
    final var future = new CompletableFuture<RCUnit>();
    this.timelineSemaphores.add(
      new TimelineSemaphore(this.device, future, semaphore)
    );
    return future;
  }

  @Override
  public RCRendererID rendererId()
  {
    return this.rendererId;
  }

  private static final class Fence
  {
    private final CompletableFuture<RCUnit> future;
    private final VulkanFenceType fence;
    private final RCDeviceType device;

    Fence(
      final RCDeviceType inDevice,
      final CompletableFuture<RCUnit> inFuture,
      final VulkanFenceType inFence)
    {
      this.device =
        Objects.requireNonNull(inDevice, "device");
      this.future =
        Objects.requireNonNull(inFuture, "future");
      this.fence =
        Objects.requireNonNull(inFence, "fence");
    }

    boolean isSignalled()
      throws RocaroException
    {
      try {
        final var vkDevice = this.device.device();
        return vkDevice.getFenceStatus(this.fence) == VK_FENCE_SIGNALLED;
      } catch (final VulkanException e) {
        throw RCVulkanException.wrap(e);
      }
    }
  }

  private static final class TimelineSemaphore
  {
    private final CompletableFuture<RCUnit> future;
    private final VulkanSemaphoreTimelineWait semaphore;
    private final RCDeviceType device;

    TimelineSemaphore(
      final RCDeviceType inDevice,
      final CompletableFuture<RCUnit> inFuture,
      final VulkanSemaphoreTimelineWait inSemaphore)
    {
      this.device =
        Objects.requireNonNull(inDevice, "device");
      this.future =
        Objects.requireNonNull(inFuture, "future");
      this.semaphore =
        Objects.requireNonNull(inSemaphore, "fence");
    }

    boolean isSignalled()
      throws RocaroException
    {
      try {
        final var vkDevice = this.device.device();
        return vkDevice.waitForTimelineSemaphore(this.semaphore, 0L)
               == VK_WAIT_SUCCEEDED;
      } catch (final VulkanException e) {
        throw RCVulkanException.wrap(e);
      }
    }
  }
}
