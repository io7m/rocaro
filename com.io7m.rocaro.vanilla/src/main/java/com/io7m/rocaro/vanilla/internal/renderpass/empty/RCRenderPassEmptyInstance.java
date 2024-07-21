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


package com.io7m.rocaro.vanilla.internal.renderpass.empty;

import com.io7m.rocaro.api.RCUnit;
import com.io7m.rocaro.api.graph.RCGNodeName;
import com.io7m.rocaro.api.graph.RCGNodeRenderContextType;
import com.io7m.rocaro.api.graph.RCGNodeRenderPassAbstract;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortType;
import com.io7m.rocaro.api.render_pass.RCRenderPassType;

import java.util.Map;

final class RCRenderPassEmptyInstance
  extends RCGNodeRenderPassAbstract<RCUnit>
  implements RCRenderPassType<RCUnit>
{
  RCRenderPassEmptyInstance(
    final RCGNodeName inName)
  {
    super(inName, RCUnit.UNIT);
  }

  @Override
  public Map<RCGPortName, RCGPortType<?>> ports()
  {
    return Map.of();
  }

  @Override
  public void evaluate(
    final RCGNodeRenderContextType context)
  {

  }
}
