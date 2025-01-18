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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.rocaro.api.devices.RCDeviceQueueCategory;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@JsonPropertyOrder(alphabetic = true)
@JsonSerialize
public final class RCTOperationDeclaration
  extends RCTDeclarationAbstract
  implements RCTDeclarationType
{
  @JsonProperty(value = "QueueCategory")
  private final RCDeviceQueueCategory queueCategory;
  private final TreeMap<RCCName, RCTPortType> ports;
  @JsonProperty(value = "Ports")
  private final SortedMap<RCCName, RCTPortType> portsRead;

  private RCTOperationDeclaration(
    final RCTGraphDeclarationType owner,
    final RCCName name,
    final RCDeviceQueueCategory inQueueCategory)
  {
    super(owner, name);

    this.queueCategory =
      Objects.requireNonNull(inQueueCategory, "queueCategory");
    this.ports =
      new TreeMap<>();
    this.portsRead =
      Collections.unmodifiableSortedMap(this.ports);
  }

  public SortedMap<RCCName, RCTPortType> ports()
  {
    return this.portsRead;
  }

  @Override
  public String toString()
  {
    return "[%s %s %s %s]"
      .formatted(
        this.getClass().getSimpleName(),
        this.name(),
        this.queueCategory,
        this.portsRead
      );
  }

  public static Builder builder(
    final RCTGraphDeclarationType owner,
    final RCCName name,
    final RCDeviceQueueCategory queueCategory)
  {
    return new Builder(owner, name, queueCategory);
  }

  public RCDeviceQueueCategory queueCategory()
  {
    return this.queueCategory;
  }

  @Override
  public String kind()
  {
    return "Operation";
  }

  @FunctionalInterface
  public interface ConsumerPortBuilderFunctionType
  {
    void configure(
      RCTPorts.ConsumerBuilder b)
      throws RCCompilerException;
  }

  @FunctionalInterface
  public interface ProducerPortBuilderFunctionType
  {
    void configure(
      RCTPorts.ProducerBuilder b)
      throws RCCompilerException;
  }

  @FunctionalInterface
  public interface ModifierPortBuilderFunctionType
  {
    void configure(
      RCTPorts.ModifierBuilder b)
      throws RCCompilerException;
  }

  public static final class Builder
  {
    private RCTOperationDeclaration target;

    private Builder(
      final RCTGraphDeclarationType owner,
      final RCCName name,
      final RCDeviceQueueCategory queueCategory)
    {
      this.target =
        new RCTOperationDeclaration(owner, name, queueCategory);
    }

    public RCTOperationDeclaration build()
    {
      this.checkNotBuilt();

      final var r = this.target;
      this.target = null;
      return r;
    }

    private void checkNotBuilt()
    {
      if (this.target == null) {
        throw new IllegalStateException("Builder already completed.");
      }
    }

    public Builder createConsumerPort(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RCTTypeDeclarationType type,
      final ConsumerPortBuilderFunctionType f)
      throws RCCompilerException
    {
      this.checkNotBuilt();

      final var b = RCTPorts.consumerBuilder(this.target, type, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.ports.put(name, rr);
      return this;
    }

    public Builder createModifierPort(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RCTTypeDeclarationType type,
      final ModifierPortBuilderFunctionType f)
      throws RCCompilerException
    {
      this.checkNotBuilt();

      final var b = RCTPorts.modifierBuilder(this.target, type, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.ports.put(name, rr);
      return this;
    }

    public Builder createProducerPort(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RCTTypeDeclarationType type,
      final ProducerPortBuilderFunctionType f)
      throws RCCompilerException
    {
      this.checkNotBuilt();

      final var b = RCTPorts.producerBuilder(this.target, type, name);
      f.configure(b);
      final var rr = b.build();
      rr.setLexical(position);
      this.target.ports.put(name, rr);
      return this;
    }
  }
}
