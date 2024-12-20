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


package com.io7m.rocaro.vanilla.internal.vulkan;

import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanLogicalDeviceCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceQueueCreateInfo;
import com.io7m.jcoronado.api.VulkanLogicalDeviceType;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanQueueFamilyIndex;
import com.io7m.jcoronado.api.VulkanQueueFamilyProperties;
import com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag;
import com.io7m.jcoronado.vma.VMAAllocatorCreateInfo;
import com.io7m.jcoronado.vma.VMAAllocatorType;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.threading.RCStandardExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_COMPUTE_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_GRAPHICS_BIT;
import static com.io7m.jcoronado.api.VulkanQueueFamilyPropertyFlag.VK_QUEUE_TRANSFER_BIT;
import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_QUEUE_MISSING;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_DEVICE_QUEUE_PRESENTATION_UNSUPPORTED;

/**
 * Functions to create logical devices.
 */

public final class RCLogicalDevices
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCLogicalDevices.class);

  private RCLogicalDevices()
  {

  }

  /**
   * Create a logical device.
   *
   * @param strings                The string resources
   * @param executors              The executors
   * @param vulkanConfiguration    The Vulkan configuration
   * @param physicalDevice         The physical device
   * @param window                 The window
   * @param requiredDeviceFeatures The required device features
   * @param rendererId             The renderer ID
   *
   * @return A logical device
   *
   * @throws RCVulkanException On errors
   */

  public static RCDevice create(
    final RCStrings strings,
    final RCStandardExecutors executors,
    final RendererVulkanConfiguration vulkanConfiguration,
    final VulkanPhysicalDeviceType physicalDevice,
    final RCWindowWithSurfaceType window,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures,
    final RCRendererID rendererId)
    throws RocaroException
  {
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(physicalDevice, "physicalDevice");
    Objects.requireNonNull(requiredDeviceFeatures, "requiredDeviceFeatures");
    Objects.requireNonNull(rendererId, "rendererId");

    /*
     * The queue requirements are:
     *
     * 1. A graphics queue.
     * 2. A transfer queue.
     * 3. A compute queue.
     * 4. A presentation queue (depending on the window!)
     *
     * It is preferable if all queues are distinct. However, it is
     * acceptable if (2) and (3) are the same queue, and it is also acceptable
     * if (1) and (4) are the same queue (and this is quite likely on most
     * GPUs).
     *
     * On integrated GPUs, there might only be a single queue covering all use
     * cases.
     *
     * We assume that the chosen physical device has all the required queues
     * already, so this code is simply responsible for determining which
     * specific queues to use.
     */

    try {
      final var extensions = new HashSet<String>();
      if (window.window().requiresSurface()) {
        extensions.add("VK_KHR_swapchain");
      }

      final var allocator =
        new QueueAllocator(physicalDevice.queueFamilies().values());

      final var graphicsFamily =
        allocator.allocateQueueFor(VK_QUEUE_GRAPHICS_BIT);
      final var transferFamily =
        allocator.allocateQueueFor(VK_QUEUE_TRANSFER_BIT);
      final var computeFamily =
        allocator.allocateQueueFor(VK_QUEUE_COMPUTE_BIT);

      final Optional<VulkanQueueFamilyIndex> presentationFamilyOpt =
        switch (window) {
          case final RCWindowWithSurface withSurface -> {
            yield Optional.of(allocator.allocatePresentationQueue(
              strings,
              physicalDevice,
              withSurface,
              List.of(graphicsFamily, computeFamily, transferFamily)
            ));
          }
          case final RCWindowWithoutSurface _ -> {
            yield Optional.empty();
          }
        };

      final var queues =
        collectQueueInfo(allocator.queueAllocated);

      final var logicalDevice =
        physicalDevice.createLogicalDevice(
          VulkanLogicalDeviceCreateInfo.builder()
            .setFeatures(requiredDeviceFeatures)
            .setEnabledExtensions(extensions)
            .setQueueCreateInfos(queues)
            .build()
        );

      return findCreatedQueues(
        strings,
        executors,
        window,
        logicalDevice,
        graphicsFamily,
        transferFamily,
        computeFamily,
        presentationFamilyOpt,
        vulkanConfiguration
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private static ArrayList<VulkanLogicalDeviceQueueCreateInfo> collectQueueInfo(
    final HashMap<VulkanQueueFamilyIndex, Integer> queueCreation)
  {
    final var queues =
      new ArrayList<VulkanLogicalDeviceQueueCreateInfo>();

    var totalCount = 0;
    for (final var entry : queueCreation.entrySet()) {
      final var count = entry.getValue();
      final var family = entry.getKey();
      LOG.debug("Queue create: Family {} Count {}", family, count);
      totalCount += count;

      final var builder =
        VulkanLogicalDeviceQueueCreateInfo.builder()
          .setQueueFamilyIndex(family);

      for (var index = 0; index < count; ++index) {
        builder.addQueuePriorities(1.0f);
      }

      queues.add(builder.build());
    }

    LOG.debug("Requesting the creation of {} queues in total.", totalCount);
    return queues;
  }

  private static RCDevice findCreatedQueues(
    final RCStrings strings,
    final RCStandardExecutors executors,
    final RCWindowWithSurfaceType window,
    final VulkanLogicalDeviceType logicalDevice,
    final VulkanQueueFamilyIndex graphicsFamily,
    final VulkanQueueFamilyIndex transferFamily,
    final VulkanQueueFamilyIndex computeFamily,
    final Optional<VulkanQueueFamilyIndex> presentationFamilyOpt,
    final RendererVulkanConfiguration configuration)
    throws VulkanException, RocaroException
  {
    final var graphicsQueue =
      logicalDevice.queues()
        .stream()
        .filter(q -> {
          return Objects.equals(q.queueFamilyIndex(), graphicsFamily);
        })
        .findFirst()
        .orElseThrow(() -> {
          return new IllegalStateException("Missing created graphics queue!");
        });

    final var transferQueue =
      logicalDevice.queues()
        .stream()
        .filter(q -> {
          return Objects.equals(q.queueFamilyIndex(), transferFamily);
        })
        .findFirst()
        .orElseThrow(() -> {
          return new IllegalStateException("Missing created transfer queue!");
        });

    final var computeQueue =
      logicalDevice.queues()
        .stream()
        .filter(q -> {
          return Objects.equals(q.queueFamilyIndex(), computeFamily);
        })
        .findFirst()
        .orElseThrow(() -> {
          return new IllegalStateException("Missing created compute queue!");
        });

    final var debugging = logicalDevice.debugging();
    if (Objects.equals(graphicsQueue, transferQueue)) {
      if (Objects.equals(transferQueue, computeQueue)) {
        LOG.debug("Platform queues: [graphics+transfer+compute]");
        debugging.setObjectName(
          graphicsQueue,
          "Queue[Graphics+Transfer+Compute]");
      } else {
        LOG.debug("Platform queues: [graphics+transfer] [compute]");
        debugging.setObjectName(graphicsQueue, "Queue[Graphics+Transfer]");
        debugging.setObjectName(computeQueue, "Queue[Compute]");
      }
    } else {
      if (Objects.equals(transferQueue, computeQueue)) {
        LOG.debug("Platform queues: [graphics] [transfer+compute]");
        debugging.setObjectName(graphicsQueue, "Queue[Graphics]");
        debugging.setObjectName(computeQueue, "Queue[Transfer+Compute]");
      } else {
        LOG.debug("Platform queues: [graphics] [transfer] [compute]");
        debugging.setObjectName(graphicsQueue, "Queue[Graphics]");
        debugging.setObjectName(transferQueue, "Queue[Transfer]");
        debugging.setObjectName(computeQueue, "Queue[Compute]");
      }
    }

    final var rcDevice =
      new RCDevice(
        strings,
        logicalDevice,
        executors.gpuExecutor(),
        graphicsQueue,
        transferQueue,
        computeQueue
      );

    final int maxFrames;
    if (presentationFamilyOpt.isPresent()) {
      final var presentationFamily =
        presentationFamilyOpt.get();
      final var presentationQueue =
        logicalDevice.queues()
          .stream()
          .filter(q -> {
            return Objects.equals(
              q.queueFamilyIndex(),
              presentationFamily
            );
          })
          .findFirst()
          .orElseThrow(() -> {
            return new IllegalStateException(
              "Missing created presentation queue!");
          });

      switch (window) {
        case final RCWindowWithSurface withSurface -> {
          withSurface.configureForLogicalDevice(
            rcDevice,
            graphicsQueue,
            presentationQueue
          );
          maxFrames = withSurface.maximumFramesInFlight();
        }
        case final RCWindowWithoutSurface _ -> {
          throw new UnreachableCodeException();
        }
      }
    } else {
      maxFrames = 2;
    }

    LOG.debug("Creating VMA allocator.");
    rcDevice.setAllocator(
      createVMAAllocator(logicalDevice, maxFrames, configuration)
    );
    return rcDevice;
  }

  private static VMAAllocatorType createVMAAllocator(
    final VulkanLogicalDeviceType logicalDevice,
    final int frameCount,
    final RendererVulkanConfiguration configuration)
    throws RCVulkanException
  {
    try {
      final var vmaAllocators =
        configuration.vmaAllocators();

      final var vmaCreateInfo =
        VMAAllocatorCreateInfo.builder()
          .setFrameInUseCount(OptionalInt.of(frameCount))
          .setLogicalDevice(logicalDevice)
          .build();

      return vmaAllocators.createAllocator(vmaCreateInfo);
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  private static RCVulkanException errorNoSupportedPresentationQueue(
    final RCStrings strings)
  {
    return new RCVulkanException(
      strings.format(ERROR_DEVICE_QUEUE_PRESENTATION_UNSUPPORTED),
      Map.of(),
      VULKAN_QUEUE_MISSING.codeName(),
      Optional.empty()
    );
  }

  private static final class QueueAllocator
  {
    private final HashMap<VulkanQueueFamilyIndex, Integer> queueAllocated;
    private final HashMap<VulkanQueueFamilyIndex, Integer> queueAvailable;
    private final HashMap<VulkanQueueFamilyIndex, VulkanQueueFamilyProperties> queueFamilies;

    QueueAllocator(
      final Collection<VulkanQueueFamilyProperties> families)
    {
      this.queueAllocated =
        new HashMap<>();
      this.queueAvailable =
        new HashMap<>(families.size());
      this.queueFamilies =
        new HashMap<>(families.size());

      for (final var family : families) {
        if (this.queueAvailable.containsKey(family.queueFamilyIndex())) {
          throw new IllegalArgumentException("Duplicate queue family.");
        }

        this.queueAvailable.put(
          family.queueFamilyIndex(), family.queueCount());
        this.queueFamilies.put(
          family.queueFamilyIndex(), family);
      }
    }

    public VulkanQueueFamilyIndex allocateQueueFor(
      final VulkanQueueFamilyPropertyFlag require)
    {
      /*
       * First, find all queue families that could potentially support the
       * allocation. Queue families must have the right flags, and must also
       * have some unallocated queues remaining.
       */

      final var candidates = new ArrayList<VulkanQueueFamilyProperties>();

      for (final var entry : this.queueAvailable.entrySet()) {
        final var familyIndex =
          entry.getKey();
        final var family =
          this.queueFamilies.get(familyIndex);
        final var available =
          entry.getValue();

        if (available > 0) {
          if (isFamilySuitable(family, require)) {
            candidates.add(family);
          }
        }
      }

      /*
       * Sort the candidates by the number of queue flags. This is a somewhat
       * arbitrary metric, but the intention is to find the queue that has
       * the smallest set of flags that includes the target flag; this will
       * hopefully mean that the queue represents a piece of hardware that's
       * the most "focused" at providing the given feature (such as a dedicated
       * DMA controller, or dedicated compute unit) and therefore might be
       * the fastest option.
       */

      candidates.sort(Comparator.comparingInt(o -> o.queueFlags().size()));

      for (final var candidate : candidates) {
        final var index = candidate.queueFamilyIndex();
        this.doIndexAllocation(index, require.toString());
        return index;
      }

      /*
       * If there were no candidates, then we need to reuse an existing
       * queue.
       */

      for (final var entry : this.queueAllocated.entrySet()) {
        final var familyIndex =
          entry.getKey();
        final var family =
          this.queueFamilies.get(familyIndex);

        if (isFamilySuitable(family, require)) {
          LOG.debug(
            "Reused an existing queue in family {} for {}",
            family.queueFamilyIndex(),
            require
          );
          return familyIndex;
        }
      }

      throw new UnreachableCodeException();
    }

    private void doIndexAllocation(
      final VulkanQueueFamilyIndex index,
      final String require)
    {
      LOG.debug("Allocated a new queue in family {} for {}", index, require);

      this.queueAvailable.compute(
        index,
        (_, count) -> {
          return count - 1;
        }
      );
      this.queueAllocated.compute(
        index,
        (_, count) -> {
          return Objects.requireNonNullElse(count, 0) + 1;
        }
      );
    }

    private static boolean isFamilySuitable(
      final VulkanQueueFamilyProperties family,
      final VulkanQueueFamilyPropertyFlag require)
    {
      if (require == VK_QUEUE_TRANSFER_BIT) {
        return family.queueFlagImpliesTransfer();
      }
      return family.queueFlags().contains(require);
    }

    public VulkanQueueFamilyIndex allocatePresentationQueue(
      final RCStrings strings,
      final VulkanPhysicalDeviceType physicalDevice,
      final RCWindowWithSurface withSurface,
      final List<VulkanQueueFamilyIndex> preferQueues)
      throws VulkanException, RCVulkanException
    {
      final var supportedQueues =
        withSurface.khrSurfaceExt()
          .surfaceSupport(physicalDevice, withSurface.surface());

      /*
       * There is no real benefit to having a separate presentation queue, so
       * we prefer presentation queues that are in the same family as
       * one of the queues we have already allocated.
       */

      for (final var queue : supportedQueues) {
        for (final var preferred : preferQueues) {
          if (preferred == queue.queueFamilyIndex()) {
            if (this.queueAllocated.containsKey(queue.queueFamilyIndex())) {
              LOG.debug(
                "Reusing queue family {} as a presentation queue.",
                queue.queueFamilyIndex()
              );
              return queue.queueFamilyIndex();
            }
          }
        }
      }

      for (final var queue : supportedQueues) {
        final var index =
          queue.queueFamilyIndex();
        final var available =
          this.queueAvailable.get(index);

        if (available > 0) {
          LOG.debug("Creating a new presentation queue in family {}.", index);
          this.doIndexAllocation(queue.queueFamilyIndex(), "presentation");
          return queue.queueFamilyIndex();
        }
      }

      throw errorNoSupportedPresentationQueue(strings);
    }
  }
}
