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


package com.io7m.rocaro.vanilla.internal.renderdoc;

import com.io7m.renderdoc_jffm.core.RenderDoc;
import com.io7m.renderdoc_jffm.core.RenderDocOptionType.HookIntoChildren;
import com.io7m.renderdoc_jffm.core.RenderDocType;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

final class RCRenderDocFFM
  extends RCObject
  implements RCRenderDocServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCRenderDocFFM.class);

  private final RenderDocType renderDoc;
  private final RCRendererID rendererId;

  private RCRenderDocFFM(
    final RenderDocType inRenderDoc,
    final RCRendererID inRendererId)
  {
    this.renderDoc =
      Objects.requireNonNull(inRenderDoc, "renderDoc");
    this.rendererId =
      Objects.requireNonNull(inRendererId, "inRendererId");
  }

  public static RCRenderDocServiceType create(
    final RCRendererID rendererId)
    throws RocaroException
  {
    try {
      final var ffm = new RCRenderDocFFM(RenderDoc.open(), rendererId);
      LOG.trace("{}", ffm.renderDoc.option(HookIntoChildren.class));
      LOG.trace("Captures: {}", ffm.renderDoc.captureFilePathTemplate());
      return ffm;
    } catch (final Throwable e) {
      throw new RCServiceException(e);
    }
  }

  @Override
  public void close()
    throws RocaroException
  {
    try {
      this.renderDoc.close();
    } catch (final IOException e) {
      throw new RCServiceException(e);
    }
  }

  @Override
  public String description()
  {
    return "RenderDoc native service.";
  }

  @Override
  public void triggerCapture()
  {
    this.renderDoc.triggerCapture();
  }

  @Override
  public RCRendererID rendererId()
  {
    return this.rendererId;
  }
}
