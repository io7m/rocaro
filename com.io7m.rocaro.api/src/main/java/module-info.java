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

/**
 * 3D rendering system (API).
 */

module com.io7m.rocaro.api
{
  requires static com.io7m.immutables.style;
  requires static org.immutables.value;
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.jaffirm.core;
  requires com.io7m.jcoronado.api;
  requires com.io7m.jcoronado.layers.khronos_validation.api;
  requires com.io7m.jcoronado.layers.lunarg_api_dump.api;
  requires com.io7m.jcoronado.vma;
  requires com.io7m.jtensors.core;
  requires com.io7m.lanark.core;
  requires com.io7m.repetoir.core;
  requires com.io7m.seltzer.api;
  requires com.io7m.verona.core;
  requires jdk.jfr;

  exports com.io7m.rocaro.api.assets;
  exports com.io7m.rocaro.api.devices;
  exports com.io7m.rocaro.api.displays;
  exports com.io7m.rocaro.api.graph;
  exports com.io7m.rocaro.api.images;
  exports com.io7m.rocaro.api.render_pass;
  exports com.io7m.rocaro.api.transfers;
  exports com.io7m.rocaro.api;
}
