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

import com.io7m.rocaro.api.displays.RCDisplayServiceType;
import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.vanilla.RCDisplays;
import com.io7m.rocaro.vanilla.Renderers;

/**
 * 3D rendering system (Vanilla implementation).
 */

module com.io7m.rocaro.vanilla
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.rocaro.api;

  requires com.io7m.jaffirm.core;
  requires com.io7m.jcoronado.allocation.tracker;
  requires com.io7m.jcoronado.api;
  requires com.io7m.jcoronado.extensions.ext_debug_utils.api;
  requires com.io7m.jcoronado.extensions.ext_layer_settings.api;
  requires com.io7m.jcoronado.extensions.khr.surface.api;
  requires com.io7m.jcoronado.layers.khronos_validation.api;
  requires com.io7m.jcoronado.lwjgl;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.jtensors.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.jxtrand.api;
  requires com.io7m.jxtrand.vanilla;
  requires com.io7m.verona.core;
  requires org.jgrapht.core;
  requires org.lwjgl.glfw;
  requires org.slf4j;

  provides RendererFactoryType
    with Renderers;
  provides RCDisplayServiceType
    with RCDisplays;

  opens com.io7m.rocaro.vanilla.internal
    to com.io7m.jxtrand.vanilla;

  exports com.io7m.rocaro.vanilla;

  exports com.io7m.rocaro.vanilla.internal
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.vanilla.internal.graph
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.vanilla.internal.windows
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.vanilla.internal.images
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.vanilla.internal.vulkan
    to com.io7m.rocaro.tests;
  opens com.io7m.rocaro.vanilla.internal.vulkan to com.io7m.jxtrand.vanilla;
  opens com.io7m.rocaro.vanilla.internal.graph to com.io7m.jxtrand.vanilla;
}
