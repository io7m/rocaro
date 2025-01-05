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


package com.io7m.rocaro.api.resources;

import java.util.Set;

/**
 * <p>
 * The type of composite resources.
 * </p>
 * <p>
 * A <i>composite</i> resource collects a set of <i>primitive</i> resources
 * into a single object. This allows applications to refer to resources in
 * a more convenient and type-safe manner. For example, applications can
 * collect a set of image resources representing the components of a
 * <i>g-buffer</i> into a composite resource, and then refer to the composite
 * resource as a value of a <i>g-buffer</i> resource type, as opposed to
 * carrying around a set of loose image resources. The composite resource
 * itself has no concrete representation on the graphics device.
 * </p>
 */

public non-sealed interface RCResourceCompositeType
  extends RCResourceType
{
  /**
   * @return The set of primitive resources that make up this resource
   */

  Set<RCResourcePrimitiveType> resources();
}
