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


package com.io7m.rocaro.tests;

import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures10;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures11;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures12;
import com.io7m.jcoronado.api.VulkanPhysicalDeviceFeatures13;
import com.io7m.rocaro.api.graph.RCGPortConsumer;
import com.io7m.rocaro.api.graph.RCGPortModifier;
import com.io7m.rocaro.api.graph.RCGPortName;
import com.io7m.rocaro.api.graph.RCGPortSourceType;
import com.io7m.rocaro.api.graph.RCGPortTargetType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionBuilderType;
import com.io7m.rocaro.api.graph.RCGraphDescriptionException;
import com.io7m.rocaro.api.images.RCImageConstraintDepth;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_NODE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.DUPLICATE_PORT_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.NONEXISTENT_FRAME_SOURCE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORTS_INCOMPATIBLE;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_CYCLIC_CONNECTION;
import static com.io7m.rocaro.api.RCStandardErrorCodes.PORT_NOT_CONNECTED;
import static com.io7m.rocaro.api.images.RCImageConstraintColorBlendable.requireBlendableWindowSizedRGBA;
import static com.io7m.rocaro.api.images.RCImageConstraintColorRenderable.requireRenderableWindowSizedRGBA;
import static com.io7m.rocaro.api.images.RCImageConstraintDepth.requireWindowSizedDepth;
import static com.io7m.rocaro.api.images.RCImageParametersBlendable.blendableWindowSizedR;
import static com.io7m.rocaro.api.images.RCImageParametersBlendable.blendableWindowSizedRGBA;
import static com.io7m.rocaro.api.images.RCImageParametersDepth.windowSizedDepth;
import static com.io7m.rocaro.api.images.RCImageParametersRenderable.renderableWindowSizedR;
import static com.io7m.rocaro.api.images.RCImageSizeExpressions.windowSizedHalf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class RCGraphDescriptionBuilderContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RCGraphDescriptionBuilderContract.class);

  private static final RCGPortName PORT_P =
    new RCGPortName("P");

  protected abstract RCGraphDescriptionBuilderType create();

  private static VulkanPhysicalDeviceFeatures noFeaturesRequired()
  {
    return VulkanPhysicalDeviceFeatures.builder()
      .setFeatures10(VulkanPhysicalDeviceFeatures10.builder().build())
      .setFeatures11(VulkanPhysicalDeviceFeatures11.builder().build())
      .setFeatures12(VulkanPhysicalDeviceFeatures12.builder().build())
      .setFeatures13(VulkanPhysicalDeviceFeatures13.builder().build())
      .build();
  }

  /**
   * Empty graphs do not validate.
   *
   * @throws Exception On errors
   */

  @Test
  public void testValidateEmpty()
    throws Exception
  {
    final var b = this.create();

    final var ex =
      assertThrows(RCGraphDescriptionException.class, b::validate);

    assertEquals(NONEXISTENT_FRAME_SOURCE.codeName(), ex.errorCode());
  }

  /**
   * Node names must be unique.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNodeUnique()
    throws Exception
  {
    final var b = this.create();

    b.declareColorBlendableImage("Image0", blendableWindowSizedR());

    final var ex =
      assertThrows(RCGraphDescriptionException.class, () -> {
        b.declareColorBlendableImage("Image0", blendableWindowSizedR());
      });

    assertEquals(DUPLICATE_NODE.codeName(), ex.errorCode());
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements0()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorBlendableImage("Image0", blendableWindowSizedR());

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireWindowSizedDepth())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements1()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareDepthImage("Image0", windowSizedDepth());

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> {
          return new RCGPortConsumer<>(
            p,
            PORT_P,
            new RCImageConstraintDepth(Optional.of(windowSizedHalf()))
          );
        }
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements2()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorRenderableImage("Image0", renderableWindowSizedR());

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireBlendableWindowSizedRGBA())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements3()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareDepthImage("Image0", windowSizedDepth());

    final var pass0 =
      b.declare("Fake", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements4()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorBlendableImage("Image0", blendableWindowSizedR());

    final var pass0 =
      b.declare("Fake", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Port requirements must be satisfied.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirements5()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorBlendableImage("Image0", blendableWindowSizedR());

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireWindowSizedDepth())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORTS_INCOMPATIBLE.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Ports cannot have multiple inputs.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirementsCardinality0()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorBlendableImage("Image0", blendableWindowSizedRGBA());

    final var pass0 =
      b.declare("Fake", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireBlendableWindowSizedRGBA())
      ));

    b.connect(
      image0.mainOutput(),
      (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
    );

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            image0.mainOutput(),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(DUPLICATE_PORT_CONNECTION.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Ports must be connected.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectRequirementsCardinality1()
    throws Exception
  {
    final var b =
      this.create();

    final var image0 =
      b.declareColorBlendableImage("Image0", blendableWindowSizedR());

    final var pass0 =
      b.declare("Fake", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortConsumer<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    b.declareFrameSource("FrameSource");
    b.declareFrameTarget("FrameTarget");

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, b::validate);

      assertEquals(PORT_NOT_CONNECTED.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Graphs cannot be cyclic.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectCyclic0()
    throws Exception
  {
    final var b =
      this.create();

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortModifier<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    final var pass1 =
      b.declare("Fake1", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortModifier<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    b.connect(
      (RCGPortSourceType<?>) pass0.ports().get(PORT_P),
      (RCGPortTargetType<?>) pass1.ports().get(PORT_P)
    );

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            (RCGPortSourceType<?>) pass1.ports().get(PORT_P),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORT_CYCLIC_CONNECTION.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  /**
   * Graphs cannot be cyclic.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectCyclic1()
    throws Exception
  {
    final var b =
      this.create();

    final var pass0 =
      b.declare("Fake0", 0, RCFakeRenderPass.of(
        noFeaturesRequired(),
        p -> new RCGPortModifier<>(p, PORT_P, requireRenderableWindowSizedRGBA())
      ));

    {
      final var ex =
        assertThrows(RCGraphDescriptionException.class, () -> {
          b.connect(
            (RCGPortSourceType<?>) pass0.ports().get(PORT_P),
            (RCGPortTargetType<?>) pass0.ports().get(PORT_P)
          );
        });

      assertEquals(PORT_CYCLIC_CONNECTION.codeName(), ex.errorCode());
      logException(ex);
    }
  }

  private static void logException(
    final RCGraphDescriptionException ex)
  {
    LOG.debug("Exception: {}", ex.getMessage());

    for (final var entry : ex.attributes().entrySet()) {
      LOG.debug("  {}: {}", entry.getKey(), entry.getValue());
    }
  }
}
