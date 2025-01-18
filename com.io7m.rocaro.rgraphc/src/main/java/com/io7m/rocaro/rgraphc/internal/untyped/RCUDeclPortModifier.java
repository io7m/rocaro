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


package com.io7m.rocaro.rgraphc.internal.untyped;

import com.io7m.rocaro.rgraphc.internal.RCCName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class RCUDeclPortModifier
  extends RCUGraphElement
  implements RCUDeclPortType
{
  private final RCCName name;
  private final HashSet<RCUMemoryAccessReadsType> reads;
  private final HashSet<RCUMemoryAccessWritesType> writes;
  private final HashSet<RCUDeclRequiresImageLayoutType> requiresImage;
  private final HashSet<RCUDeclEnsuresImageLayoutType> ensuresImage;
  private final Set<RCUDeclRequiresImageLayoutType> requiresImageRead;
  private final Set<RCUDeclEnsuresImageLayoutType> ensuresImageRead;
  private RCUTypeReference type;

  public RCUDeclPortModifier(
    final RCCName inName)
  {
    this.name =
      Objects.requireNonNull(inName, "name");

    this.reads =
      new HashSet<>();
    this.writes =
      new HashSet<>();

    this.requiresImage =
      new HashSet<>();
    this.ensuresImage =
      new HashSet<>();
    this.requiresImageRead =
      Collections.unmodifiableSet(this.requiresImage);
    this.ensuresImageRead =
      Collections.unmodifiableSet(this.ensuresImage);
  }

  @Override
  public Set<RCUDeclEnsuresImageLayoutType> ensuresImageLayout()
  {
    return this.ensuresImageRead;
  }

  @Override
  public Set<RCUDeclRequiresImageLayoutType> requiresImageLayout()
  {
    return this.requiresImageRead;
  }

  @Override
  public RCCName name()
  {
    return this.name;
  }

  @Override
  public void setType(
    final RCUTypeReference ref)
  {
    this.type = Objects.requireNonNull(ref, "type");
  }

  @Override
  public RCUTypeReference type()
  {
    if (this.type == null) {
      throw new IllegalStateException("Type is unset.");
    }
    return this.type;
  }

  @Override
  public Set<RCUMemoryAccessReadsType> readsAt()
  {
    return Set.copyOf(this.reads);
  }

  @Override
  public void addReadsAt(
    final RCUMemoryAccessReadsType m)
  {
    this.reads.add(Objects.requireNonNull(m, "m"));
  }

  @Override
  public Set<RCUMemoryAccessWritesType> writesAt()
  {
    return Set.copyOf(this.writes);
  }

  @Override
  public void addWritesAt(
    final RCUMemoryAccessWritesType m)
  {
    this.writes.add(Objects.requireNonNull(m, "m"));
  }

  @Override
  public void addRequiresImageLayout(
    final RCUDeclRequiresImageLayoutType r)
  {
    this.requiresImage.add(Objects.requireNonNull(r, "r"));
  }

  @Override
  public void addEnsuresImageLayout(
    final RCUDeclEnsuresImageLayoutType e)
  {
    this.ensuresImage.add(Objects.requireNonNull(e, "e"));
  }
}
