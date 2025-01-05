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


package com.io7m.rocaro.vanilla.internal.frames;

import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.rocaro.api.RCCloseableType;
import com.io7m.rocaro.api.RCFrameIndex;
import com.io7m.rocaro.api.RCFrameInformation;
import com.io7m.rocaro.api.RCFrameNumber;
import com.io7m.rocaro.api.RCObject;
import com.io7m.rocaro.api.RCRendererID;
import com.io7m.rocaro.api.RocaroException;
import com.io7m.rocaro.vanilla.internal.RCServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;

/**
 * The frame service.
 */

public final class RCFrameService
  extends RCObject
  implements RCFrameServiceType, RCCloseableType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCFrameService.class);

  private static final Attributes ATTRIBUTES =
    Attributes.create(ex -> LOG.debug("Attribute exception: ", ex));

  private final AttributeType<RCFrameInformation> frameInformation;
  private final RCRendererID rendererId;

  private RCFrameService(
    final RCFrameInformation inFrameInformation,
    final RCRendererID inRendererId)
  {
    this.frameInformation =
      ATTRIBUTES.withValue(inFrameInformation);
    this.rendererId =
      Objects.requireNonNull(inRendererId, "rendererId");
  }

  /**
   * Create a frame service.
   *
   * @param rendererId The renderer ID
   *
   * @return The service
   *
   * @throws RCServiceException On errors
   */

  public static RCFrameService create(
    final RCRendererID rendererId)
    throws RCServiceException
  {
    return new RCFrameService(
      new RCFrameInformation(
        new RCFrameNumber(BigInteger.ZERO),
        new RCFrameIndex(0)
      ),
      rendererId
    );
  }

  @Override
  public void close()
    throws RocaroException
  {
    LOG.debug("Close");
  }

  @Override
  public AttributeReadableType<RCFrameInformation> frameInformation()
  {
    return this.frameInformation;
  }

  @Override
  public void beginNewFrame(
    final RCFrameInformation newInformation)
  {
    this.frameInformation.set(newInformation);
  }

  @Override
  public String description()
  {
    return "Frame information service.";
  }

  @Override
  public RCRendererID rendererId()
  {
    return this.rendererId;
  }
}
