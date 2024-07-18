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


package com.io7m.rocaro.vanilla.internal;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vector3I;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.api.displays.RCDisplay;
import com.io7m.rocaro.api.displays.RCDisplayMode;
import com.io7m.rocaro.vanilla.internal.vulkan.RCVulkanException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVulkan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import static com.io7m.rocaro.api.RCStandardErrorCodes.VULKAN;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_GLFW_INIT;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_UNSUPPORTED;
import static com.io7m.rocaro.vanilla.internal.RCStringConstants.ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION;

/**
 * A facade to an underlying GLFW implementation.
 */

public final class RCGLFWFacade implements RCGLFWFacadeType
{
  private static final GLFWErrorCallback GLFW_ERROR_CALLBACK =
    GLFWErrorCallback.createPrint();

  private static final ReentrantLock INSTANCE_LOCK =
    new ReentrantLock();

  private static RCGLFWFacadeType INSTANCE;

  private RCGLFWFacade()
  {

  }

  /**
   * Retrieve a reference to the GLFW facade.
   *
   * @param strings The string resources
   *
   * @return The facade
   *
   * @throws RocaroException On errors
   */

  public static RCGLFWFacadeType get(
    final RCStrings strings)
    throws RocaroException
  {
    INSTANCE_LOCK.lock();

    try {
      if (INSTANCE == null) {
        INSTANCE = create(strings);
      }
      return INSTANCE;
    } finally {
      INSTANCE_LOCK.unlock();
    }
  }

  private static RCGLFWFacade create(
    final RCStrings strings)
    throws RocaroException
  {
    Objects.requireNonNull(strings, "strings");

    GLFW_ERROR_CALLBACK.set();

    if (!GLFW.glfwInit()) {
      throw errorGLFWInit(strings);
    }

    if (!GLFWVulkan.glfwVulkanSupported()) {
      throw errorVulkanUnsupported(strings);
    }

    /*
     * Specify NO_API: If this is not done, trying to use the KHR_surface
     * extension will result in a VK_ERROR_NATIVE_WINDOW_IN_USE_KHR error code.
     */

    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
    return new RCGLFWFacade();
  }

  private static RocaroException errorGLFWInit(
    final RCStrings strings)
  {
    return new RCVulkanException(
      strings.format(ERROR_GLFW_INIT),
      Map.of(),
      VULKAN.codeName(),
      Optional.empty()
    );
  }

  private static RocaroException errorVulkanUnsupported(
    final RCStrings strings)
  {
    return new RCVulkanException(
      strings.format(ERROR_VULKAN_UNSUPPORTED),
      Map.of(),
      VULKAN.codeName(),
      Optional.of(strings.format(ERROR_VULKAN_VERSION_UNSUPPORTED_REMEDIATION))
    );
  }

  private static RCDisplay loadDisplay(
    final long monitorPointer,
    final boolean primary)
  {
    final var widthMM =
      new int[1];
    final var heightMM =
      new int[1];
    final var name =
      GLFW.glfwGetMonitorName(monitorPointer);

    GLFW.glfwGetMonitorPhysicalSize(monitorPointer, widthMM, heightMM);

    final var modeBuffer =
      GLFW.glfwGetVideoModes(monitorPointer);

    final var modes = new ArrayList<RCDisplayMode>();
    modeBuffer.forEach(mode -> {
      modes.add(
        new RCDisplayMode(
          Vector2I.of(
            mode.width(),
            mode.height()
          ),
          Vector3I.of(
            mode.redBits(),
            mode.greenBits(),
            mode.blueBits()
          ),
          mode.refreshRate()
        )
      );
    });

    return new RCDisplay(
      primary,
      monitorPointer,
      name,
      Vector2D.of(widthMM[0], heightMM[0]),
      List.copyOf(modes)
    );
  }

  @Override
  public List<RCDisplay> displays()
  {
    final var pointers =
      GLFW.glfwGetMonitors();

    final var displays =
      new ArrayList<RCDisplay>();

    final var primaryDisplay =
      loadDisplay(GLFW.glfwGetPrimaryMonitor(), true);

    displays.add(primaryDisplay);
    for (var index = 0; index < pointers.capacity(); ++index) {
      final var display =
        loadDisplay(pointers.get(index), false);

      if (!Objects.equals(display.name(), primaryDisplay.name())) {
        displays.add(display);
      }
    }
    return List.copyOf(displays);
  }

  @Override
  public long windowCreateWindowed(
    final int width,
    final int height,
    final String title)
  {
    Objects.requireNonNull(title, "title");
    return GLFW.glfwCreateWindow(width, height, title, 0L, 0L);
  }

  @Override
  public long windowCreateFullscreen(
    final int width,
    final int height,
    final String title,
    final long displayID)
  {
    Objects.requireNonNull(title, "title");
    return GLFW.glfwCreateWindow(width, height, title, displayID, 0L);
  }

  @Override
  public SortedSet<String> requiredExtensions()
  {
    final var glfwRequiredExtensions =
      GLFWVulkan.glfwGetRequiredInstanceExtensions();

    final var required = new TreeSet<String>();
    for (var index = 0; index < glfwRequiredExtensions.capacity(); ++index) {
      glfwRequiredExtensions.position(index);
      required.add(glfwRequiredExtensions.getStringASCII());
    }
    return Collections.unmodifiableNavigableSet(required);
  }

  @Override
  public void windowDestroy(
    final long address)
  {
    GLFW.glfwDestroyWindow(address);
  }

  @Override
  public Vector2I windowSize(
    final long address)
  {
    final var width = new int[1];
    final var height = new int[1];

    GLFW.glfwGetWindowSize(address, width, height);
    return Vector2I.of(width[0], height[0]);
  }
}
