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
 * 3D rendering system (Render graph compiler).
 */

module com.io7m.rocaro.rgraphc
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.rocaro.api;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.io7m.anethum.api;
  requires com.io7m.blackthorne.core;
  requires com.io7m.blackthorne.jxe;
  requires com.io7m.jaffirm.core;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.jlexing.core;
  requires com.io7m.jtensors.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.jxe.core;
  requires java.net.http;
  requires java.xml;
  requires org.jgrapht.core;
  requires org.jgrapht.io;
  requires org.slf4j;

  opens com.io7m.rocaro.rgraphc.internal.typed
    to com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal.access_set
    to com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal to
    com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal.primitive_tree
    to com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal.primitive_graph
    to com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal.primitive_sync
    to com.fasterxml.jackson.databind;
  opens com.io7m.rocaro.rgraphc.internal.json to
    com.fasterxml.jackson.databind;

  exports com.io7m.rocaro.rgraphc;

  exports com.io7m.rocaro.rgraphc.internal.parser
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.checker
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.loader
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.untyped
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.typed
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal
    to com.fasterxml.jackson.databind, com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.primitive_tree
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.primitive_graph
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.primitive_sync
    to com.io7m.rocaro.tests;
  exports com.io7m.rocaro.rgraphc.internal.json to
    com.fasterxml.jackson.databind, com.io7m.rocaro.tests;
}
