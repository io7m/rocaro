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

import com.io7m.jcoronado.allocation_tracker.VulkanHostAllocatorTracker;
import com.io7m.jcoronado.api.VulkanApplicationInfo;
import com.io7m.jcoronado.api.VulkanCommandPoolType;
import com.io7m.jcoronado.api.VulkanException;
import com.io7m.jcoronado.api.VulkanExtensionProperties;
import com.io7m.jcoronado.api.VulkanExtensions;
import com.io7m.jcoronado.api.VulkanInstanceCreateInfo;
import com.io7m.jcoronado.api.VulkanInstanceProviderType;
import com.io7m.jcoronado.api.VulkanInstanceType;
import com.io7m.jcoronado.api.VulkanLayerProperties;
import com.io7m.jcoronado.api.VulkanLayers;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceType;
import com.io7m.jcoronado.api.VulkanVersion;
import com.io7m.jcoronado.api.VulkanVersions;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessageSeverityFlag;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessageTypeFlag;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsMessengerCreateInfoEXT;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsSLF4J;
import com.io7m.jcoronado.extensions.ext_debug_utils.api.VulkanDebugUtilsType;
import com.io7m.jcoronado.lwjgl.VulkanLWJGLHostAllocatorJeMalloc;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RendererVulkanConfiguration;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplaySelectionType;
import com.io7m.rocaro.vanilla.internal.RCGLFWFacadeType;
import com.io7m.rocaro.vanilla.internal.RCObject;
import com.io7m.rocaro.vanilla.internal.RCStrings;
import com.io7m.rocaro.vanilla.internal.RCVersions;
import com.io7m.rocaro.vanilla.internal.windows.RCWindowType;
import com.io7m.rocaro.vanilla.internal.windows.RCWindows;
import com.io7m.verona.core.Version;
import com.io7m.verona.core.VersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN_VERSION_UNSUPPORTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_VERSION_UNSUPPORTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.VERSION_PROVIDED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.VERSION_REQUIRED;

/**
 * The Vulkan portion of the renderer.
 */

public final class RCVulkanRenderer
  extends RCObject
  implements RCVulkanRendererType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCVulkanRenderer.class);

  private static final int VULKAN_API_VERSION =
    VulkanVersions.encode(1, 3, 0);
  private static final String VK_LAYER_KHRONOS_VALIDATION =
    "VK_LAYER_KHRONOS_validation";
  private static final String VK_EXT_DEBUG_UTILS =
    "VK_EXT_debug_utils";

  private final RendererVulkanConfiguration configuration;
  private final VulkanInstanceType instance;
  private final RCWindowType window;
  private final RCWindowWithSurfaceType windowWithSurface;
  private final VulkanPhysicalDeviceType physicalDevice;
  private final RCLogicalDevice logicalDevice;
  private final Map<RCFrameIndex, RCVulkanFrameStateType> frameStates;

  private RCVulkanRenderer(
    final RendererVulkanConfiguration inConfiguration,
    final VulkanInstanceType inInstance,
    final RCWindowType inWindow,
    final RCWindowWithSurfaceType inWindowWithSurface,
    final VulkanPhysicalDeviceType inPhysicalDevice,
    final RCLogicalDevice inLogicalDevice,
    final Map<RCFrameIndex, RCVulkanFrameStateType> inFrameStates)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.instance =
      Objects.requireNonNull(inInstance, "instance");
    this.window =
      Objects.requireNonNull(inWindow, "window");
    this.windowWithSurface =
      Objects.requireNonNull(inWindowWithSurface, "windowWithSurface");
    this.physicalDevice =
      Objects.requireNonNull(inPhysicalDevice, "physicalDevice");
    this.logicalDevice =
      Objects.requireNonNull(inLogicalDevice, "logicalDevice");
    this.frameStates =
      Objects.requireNonNull(inFrameStates, "frameStates");
  }

  /**
   * The Vulkan portion of the renderer.
   *
   * @param strings                The strings
   * @param versions               The versions
   * @param glfw                   The GLFW facade
   * @param resources              The resources
   * @param configuration          The configuration
   * @param displaySelection       The display selection method
   * @param requiredDeviceFeatures The required device features
   *
   * @return The renderer
   *
   * @throws RocaroException On errors
   */

  public static RCVulkanRendererType create(
    final RCStrings strings,
    final RCVersions versions,
    final RCGLFWFacadeType glfw,
    final CloseableCollectionType<RocaroException> resources,
    final RendererVulkanConfiguration configuration,
    final RCDisplaySelectionType displaySelection,
    final VulkanPhysicalDeviceFeatures requiredDeviceFeatures)
    throws RocaroException
  {
    Objects.requireNonNull(strings, "strings");
    Objects.requireNonNull(glfw, "glfw");
    Objects.requireNonNull(versions, "versions");
    Objects.requireNonNull(resources, "resources");
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(displaySelection, "displaySelection");
    Objects.requireNonNull(requiredDeviceFeatures, "requiredDeviceFeatures");

    LOG.debug("Creating window.");
    final var window =
      resources.add(RCWindows.create(strings, glfw, displaySelection));
    LOG.debug("Created window {}", window);

    LOG.debug("Creating Vulkan instance.");
    final var hostAllocatorMain =
      new VulkanLWJGLHostAllocatorJeMalloc();
    final var hostAllocatorTracker =
      new VulkanHostAllocatorTracker(hostAllocatorMain);
    final var instances =
      configuration.instanceProvider();

    LOG.debug(
      "Instance provider: {} {}",
      instances.providerName(),
      instances.providerVersion()
    );

    checkAcceptableVulkanVersion(strings, instances);

    /*
     * Configure all the various extensions and layers.
     */

    final var enableExtensions =
      configureInstanceExtensions(
        strings,
        instances,
        glfw,
        window,
        configuration);
    final var enableLayers =
      configureInstanceLayers(strings, instances, configuration);

    /*
     * Create a new instance.
     */

    final VulkanInstanceType instance;
    try {
      instance = createInstance(
        versions,
        resources,
        configuration,
        enableExtensions,
        enableLayers,
        instances,
        hostAllocatorTracker
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(strings, e);
    }
    LOG.debug("Created Vulkan instance.");

    configureDebugging(strings, resources, instance);

    LOG.debug("Creating rendering surface.");
    final var windowWithSurface =
      resources.add(
        RCSurfaces.createWindowWithSurface(
          strings,
          instance,
          window
        )
      );

    LOG.debug("Creating physical device.");
    final var physicalDevice =
      resources.add(
        RCPhysicalDevices.create(
          strings,
          instance,
          requiredDeviceFeatures,
          configuration.deviceSelection(),
          windowWithSurface
        )
      );

    LOG.debug("Configuring surface for selected physical device.");
    windowWithSurface.configureForPhysicalDevice(physicalDevice);

    LOG.debug("Creating logical device.");
    final var logicalDevice =
      resources.add(
        RCLogicalDevices.create(
          strings,
          physicalDevice,
          windowWithSurface,
          requiredDeviceFeatures
        )
      );

    /*
     * We re-add the window to the resources here because, although the window
     * was registered as a to-be-closed resource early in the initialization
     * process, it may have acquired extra resources (such as a swap chain)
     * that need to be destroyed prior to the logical device being destroyed.
     */

    resources.add(windowWithSurface);

    /*
     * Set up the per-frame rendering state.
     */

    final var maxFrames =
      windowWithSurface.maximumFramesInFlight();
    final var frameStates =
      new HashMap<RCFrameIndex, RCVulkanFrameStateType>(maxFrames);

    for (var index = 0; index < maxFrames; ++index) {
      final var frameIndex =
        new RCFrameIndex(index);
      final var frameState =
        resources.add(
          RCVulkanFrameState.create(
            strings,
            logicalDevice,
            frameIndex
          )
        );
      frameStates.put(frameIndex, frameState);
    }

    return new RCVulkanRenderer(
      configuration,
      instance,
      window,
      windowWithSurface,
      physicalDevice,
      logicalDevice,
      frameStates
    );
  }

  private static void configureDebugging(
    final RCStrings strings,
    final CloseableCollectionType<RocaroException> resources,
    final VulkanInstanceType instance)
    throws RCVulkanException
  {
    try {
      final var debugOpt =
        instance.findEnabledExtension(
          VK_EXT_DEBUG_UTILS,
          VulkanDebugUtilsType.class
        );

      if (debugOpt.isEmpty()) {
        LOG.warn("Extension {} is unavailable", VK_EXT_DEBUG_UTILS);
        return;
      }

      final var debug = debugOpt.orElseThrow();
      resources.add(
        debug.createDebugUtilsMessenger(
          instance,
          VulkanDebugUtilsMessengerCreateInfoEXT.builder()
            .setSeverity(EnumSet.allOf(VulkanDebugUtilsMessageSeverityFlag.class))
            .setType(EnumSet.allOf(VulkanDebugUtilsMessageTypeFlag.class))
            .setCallback(new VulkanDebugUtilsSLF4J(LOG))
            .build()
        )
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(strings, e);
    }
  }

  private static VulkanInstanceType createInstance(
    final RCVersions versions,
    final CloseableCollectionType<RocaroException> resources,
    final RendererVulkanConfiguration configuration,
    final Set<String> enableExtensions,
    final Set<String> enableLayers,
    final VulkanInstanceProviderType instances,
    final VulkanHostAllocatorTracker hostAllocatorTracker)
    throws VulkanException
  {
    final Version engineVersion;
    try {
      engineVersion = versions.engineVersion();
    } catch (final VersionException e) {
      throw new IllegalStateException(e);
    }

    final var applicationVersion =
      configuration.applicationVersion();

    final var applicationInfo =
      VulkanApplicationInfo.builder()
        .setApplicationName(configuration.applicationName())
        .setApplicationVersion(
          VulkanVersions.encode(
            applicationVersion.major(),
            applicationVersion.minor(),
            applicationVersion.patch()
          )
        )
        .setEngineName("com.io7m.rocaro")
        .setEngineVersion(
          VulkanVersions.encode(
            engineVersion.major(),
            engineVersion.minor(),
            engineVersion.patch()
          )
        )
        .setVulkanAPIVersion(VULKAN_API_VERSION)
        .build();

    final var instanceCreateInfo =
      VulkanInstanceCreateInfo.builder()
        .setApplicationInfo(applicationInfo)
        .setEnabledExtensions(enableExtensions)
        .setEnabledLayers(enableLayers)
        .build();

    return resources.add(
      instances.createInstance(
        instanceCreateInfo,
        Optional.of(hostAllocatorTracker)
      )
    );
  }

  private static Set<String> configureInstanceLayers(
    final RCStrings strings,
    final VulkanInstanceProviderType instances,
    final RendererVulkanConfiguration configuration)
    throws RCVulkanException
  {
    try {
      final var layers =
        instances.layers()
          .values()
          .stream()
          .sorted(Comparator.comparing(VulkanLayerProperties::name))
          .toList();

      for (final var layer : layers) {
        final var specVersion =
          VulkanVersions.decode(layer.specificationVersion());
        final var implVersion =
          VulkanVersions.decode(layer.implementationVersion());

        LOG.debug(
          "Layer (Available): {} (Specification {}.{}.{}) (Implementation {}.{}.{})",
          layer.name(),
          specVersion.major(),
          specVersion.minor(),
          specVersion.patch(),
          implVersion.major(),
          implVersion.minor(),
          implVersion.patch()
        );
      }

      final var optional =
        new TreeSet<String>();
      final var required =
        new TreeSet<String>();

      if (configuration.enableValidation()) {
        required.add(VK_LAYER_KHRONOS_VALIDATION);
      }

      final var enable =
        VulkanLayers.filterRequiredLayers(
          instances.layers(), optional, required);

      for (final var layer : enable) {
        LOG.debug("Layer (Enable): {}", layer);
      }

      return enable;
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(strings, e);
    }
  }

  private static Set<String> configureInstanceExtensions(
    final RCStrings strings,
    final VulkanInstanceProviderType instances,
    final RCGLFWFacadeType glfw,
    final RCWindowType window,
    final RendererVulkanConfiguration configuration)
    throws RCVulkanException
  {
    try {
      final var extensions =
        instances.extensions()
          .values()
          .stream()
          .sorted(Comparator.comparing(VulkanExtensionProperties::name))
          .toList();

      for (final var extension : extensions) {
        LOG.debug(
          "Extension (Available): {} {}",
          extension.name(),
          extension.version());
      }

      final var optional =
        new TreeSet<String>();
      final var required =
        new TreeSet<String>();

      /*
       * Only enable windowing-system-required extensions if we have a window
       * type that actually requires a surface. Purely offscreen rendering,
       * for example, can be performed without any interaction with the
       * display.
       */

      if (window.requiresSurface()) {
        final var windowSystemRequired = glfw.requiredExtensions();
        for (final var extension : windowSystemRequired) {
          LOG.debug("Extension (Required by window system): {}", extension);
        }
        required.addAll(windowSystemRequired);
      }

      optional.add(VK_EXT_DEBUG_UTILS);

      final var enable =
        VulkanExtensions.filterRequiredExtensions(
          instances.extensions(),
          optional,
          required
        );

      for (final var extension : enable) {
        LOG.debug("Extension (Enable): {}", extension);
      }

      return enable;
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(strings, e);
    }
  }

  private static void checkAcceptableVulkanVersion(
    final RCStrings strings,
    final VulkanInstanceProviderType instances)
    throws RocaroException
  {
    final var supported = instances.findSupportedInstanceVersion();
    LOG.debug(
      "Supported Vulkan version: {}.{}.{}",
      supported.major(),
      supported.minor(),
      supported.patch()
    );

    if (supported.major() < 1) {
      throw errorVulkanVersionNotSupported(strings, supported);
    }

    if (supported.major() == 1) {
      if (supported.minor() < 3) {
        throw errorVulkanVersionNotSupported(strings, supported);
      }
    }
  }

  private static RocaroException errorVulkanVersionNotSupported(
    final RCStrings strings,
    final VulkanVersion supported)
  {
    return new RCVulkanException(
      strings.format(ERROR_VULKAN_VERSION_UNSUPPORTED),
      Map.ofEntries(
        Map.entry(
          strings.format(VERSION_PROVIDED),
          "%s.%s.%s".formatted(
            Integer.toUnsignedString(supported.major()),
            Integer.toUnsignedString(supported.minor()),
            Integer.toUnsignedString(supported.patch())
          )
        ),
        Map.entry(
          strings.format(VERSION_REQUIRED),
          "At least 1.3.0."
        )
      ),
      VULKAN_VERSION_UNSUPPORTED.codeName(),
      Optional.of(strings.format(ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION))
    );
  }

  @Override
  public VulkanInstanceType instance()
  {
    return this.instance;
  }

  @Override
  public RCLogicalDevice logicalDevice()
  {
    return this.logicalDevice;
  }

  @Override
  public VulkanPhysicalDeviceType physicalDevice()
  {
    return this.physicalDevice;
  }

  @Override
  public RCWindowType window()
  {
    return this.window;
  }

  @Override
  public RCWindowWithSurfaceType windowWithSurface()
  {
    return this.windowWithSurface;
  }

  @Override
  public RCVulkanFrameContextType acquireFrame(
    final RCFrameIndex frame)
    throws RCVulkanException, TimeoutException
  {
    Objects.requireNonNull(frame, "frame");

    try {
      final var windowContext =
        this.windowWithSurface.acquireFrame(
          frame,
          this.configuration.imageAcquisitionTimeout()
        );

      final var frameState =
        this.frameStates.get(frame);

      this.logicalDevice.device()
        .resetCommandPool(frameState.commandPool());

      return new FrameContext(
        windowContext,
        this.logicalDevice,
        frameState
      );
    } catch (final VulkanException e) {
      throw RCVulkanException.wrap(e);
    }
  }

  @Override
  public int maximumFramesInFlight()
  {
    return this.windowWithSurface.maximumFramesInFlight();
  }

  private static final class FrameContext
    implements RCVulkanFrameContextType
  {
    private final RCWindowFrameContextType windowFrameContext;
    private final RCLogicalDevice logicalDevice;
    private final RCVulkanFrameStateType frameState;

    private FrameContext(
      final RCWindowFrameContextType inWindowFrameContext,
      final RCLogicalDevice inLogicalDevice,
      final RCVulkanFrameStateType inFrameState)
    {
      this.windowFrameContext =
        Objects.requireNonNull(inWindowFrameContext, "windowFrameContext");
      this.logicalDevice =
        Objects.requireNonNull(inLogicalDevice, "logicalDevice");
      this.frameState =
        Objects.requireNonNull(inFrameState, "frameState");
    }

    @Override
    public RCLogicalDevice device()
    {
      return this.logicalDevice;
    }

    @Override
    public RCWindowFrameContextType windowFrameContext()
    {
      return this.windowFrameContext;
    }

    @Override
    public VulkanCommandPoolType commandPool()
    {
      return this.frameState.commandPool();
    }

    @Override
    public void close()
      throws RocaroException
    {
      this.windowFrameContext.close();
    }
  }
}
