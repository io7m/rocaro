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


package com.io7m.rocaro.rgraphc.internal.typed;

import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPTreeType;
import com.io7m.rocaro.rgraphc.internal.primitive_tree.RCTPrimitiveTree;

public final class RCTTypeDeclarations
{
  private RCTTypeDeclarations()
  {

  }

  public static RCTPTreeType primitiveTreeOf(
    final RCTTypeDeclarationType type)
  {
    return switch (type) {
      case final RCTTypeDeclarationBuffer b -> {
        yield RCTPrimitiveTree.singleton(b);
      }

      case final RCTTypeDeclarationImage i -> {
        yield RCTPrimitiveTree.singleton(i);
      }

      case final RCTTypeDeclarationRecord r -> {
        final var tb = RCTPrimitiveTree.builder();
        for (final var f : r.fields().values()) {
          switch (f.type()) {
            case final RCTTypeDeclarationBuffer b -> {
              tb.addResource(f.name(), b);
            }
            case final RCTTypeDeclarationImage i -> {
              tb.addResource(f.name(), i);
            }
            case final RCTTypeDeclarationRecord rr -> {
              primitiveBranchOf(tb.addBranch(f.name()), rr);
            }
            case final RCTTypeDeclarationRenderTarget rt -> {
              primitiveBranchOf(tb.addBranch(f.name()), rt);
            }
          }
        }
        yield tb.build();
      }

      case final RCTTypeDeclarationRenderTarget rt -> {
        final var tb = RCTPrimitiveTree.builder();
        for (final var entry : rt.allAttachments().entrySet()) {
          tb.addResource(entry.getKey(), entry.getValue());
        }
        yield tb.build();
      }
    };
  }

  private static void primitiveBranchOf(
    final RCTPrimitiveTree.BranchBuilder branchBuilder,
    final RCTTypeDeclarationRecord r)
  {
    for (final var f : r.fields().values()) {
      switch (f.type()) {
        case final RCTTypeDeclarationBuffer b -> {
          branchBuilder.addResource(f.name(), b);
        }
        case final RCTTypeDeclarationImage i -> {
          branchBuilder.addResource(f.name(), i);
        }
        case final RCTTypeDeclarationRecord rr -> {
          primitiveBranchOf(branchBuilder.addBranch(f.name()), rr);
        }
        case final RCTTypeDeclarationRenderTarget rt -> {
          primitiveBranchOf(branchBuilder.addBranch(f.name()), rt);
        }
      }
    }
  }

  private static void primitiveBranchOf(
    final RCTPrimitiveTree.BranchBuilder branchBuilder,
    final RCTTypeDeclarationRenderTarget rt)
  {
    for (final var entry : rt.allAttachments().entrySet()) {
      branchBuilder.addResource(entry.getKey(), entry.getValue());
    }
  }
}
