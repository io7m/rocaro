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

import com.io7m.rocaro.api.RendererFactoryType;
import com.io7m.rocaro.api.displays.RCDisplayServiceType;

/**
 * 3D rendering system (Test suite).
 */

open module com.io7m.rocaro.tests
{
  requires com.io7m.rocaro.api;
  requires com.io7m.rocaro.vanilla;
  requires com.io7m.rocaro.tests.arbitraries;

  requires org.junit.jupiter.api;
  requires org.junit.jupiter.engine;
  requires org.junit.platform.commons;
  requires org.junit.platform.engine;
  requires org.junit.platform.launcher;

  requires com.io7m.jcoronado.api;
  requires com.io7m.jcoronado.extensions.khr.surface.api;
  requires com.io7m.jcoronado.fake;
  requires com.io7m.jcoronado.layers.khronos_validation.api;
  requires com.io7m.jcoronado.lwjgl;
  requires com.io7m.jcoronado.vma;
  requires com.io7m.jtensors.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.percentpass.extension;
  requires net.jqwik.api;
  requires net.jqwik.engine;
  requires org.mockito;
  requires org.slf4j;

  uses RendererFactoryType;
  uses RCDisplayServiceType;

  exports com.io7m.rocaro.tests;
  exports com.io7m.rocaro.tests.integration;
  exports com.io7m.rocaro.tests.graph2;
}
