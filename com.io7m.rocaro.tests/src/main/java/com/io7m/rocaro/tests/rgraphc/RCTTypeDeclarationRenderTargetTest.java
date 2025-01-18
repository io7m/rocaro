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


package com.io7m.rocaro.tests.rgraphc;

import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.rocaro.rgraphc.internal.typed.RCTTypeDeclarationRenderTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RCTTypeDeclarationRenderTargetTest
{
  private RCTGraphDeclarationType graph;

  @BeforeEach
  public void setup()
  {
    this.graph =
      Mockito.mock(RCTGraphDeclarationType.class);
  }

  @Test
  public void testEmpty()
  {
    final var rt =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      ).build();

    assertEquals(Collections.emptySortedMap(), rt.allAttachments());
    assertEquals(Collections.emptySortedMap(), rt.colorAttachmentsByIndex());
    assertEquals(Collections.emptySortedMap(), rt.colorAttachmentsByName());
    assertEquals(Optional.empty(), rt.depthAttachment());
  }

  @Test
  public void testAlreadyBuilt()
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
      this.graph,
      new RCCName("RT")
    );

    final var rt = b.build();
    assertThrows(IllegalStateException.class, b::build);
  }

  @Test
  public void testDepthAlreadySet()
    throws RCCompilerException
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      );

    b.setDepthAttachment(LexicalPositions.zero(), new RCCName("D"));

    assertThrows(
      RCCompilerException.class,
      () -> {
        b.setDepthAttachment(LexicalPositions.zero(), new RCCName("D"));
      }
    );
  }

  @Test
  public void testNameDuplicate0()
    throws RCCompilerException
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      );

    b.setDepthAttachment(LexicalPositions.zero(), new RCCName("D"));

    assertThrows(
      RCCompilerException.class,
      () -> {
        b.addColorAttachment(
          LexicalPositions.zero(),
          new RCCName("D"),
          1
        );
      }
    );
  }

  @Test
  public void testNameDuplicate1()
    throws RCCompilerException
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      );

    b.addColorAttachment(
      LexicalPositions.zero(),
      new RCCName("D"),
      1
    );

    assertThrows(
      RCCompilerException.class,
      () -> {
        b.setDepthAttachment(LexicalPositions.zero(), new RCCName("D"));
      }
    );
  }

  @Test
  public void testColorDuplicate0()
    throws RCCompilerException
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      );

    b.addColorAttachment(
      LexicalPositions.zero(),
      new RCCName("C"),
      0
    );

    assertThrows(
      RCCompilerException.class,
      () -> {
        b.addColorAttachment(
          LexicalPositions.zero(),
          new RCCName("C"),
          1
        );
      }
    );
  }

  @Test
  public void testColorDuplicate1()
    throws RCCompilerException
  {
    final var b =
      RCTTypeDeclarationRenderTarget.builder(
        this.graph,
        new RCCName("RT")
      );

    b.addColorAttachment(
      LexicalPositions.zero(),
      new RCCName("C"),
      0
    );

    assertThrows(
      RCCompilerException.class,
      () -> {
        b.addColorAttachment(
          LexicalPositions.zero(),
          new RCCName("D"),
          0
        );
      }
    );
  }
}
