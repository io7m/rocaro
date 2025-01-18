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
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.rocaro.rgraphc.internal.RCCName;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.seltzer.api.SStructuredError;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.io7m.rocaro.rgraphc.internal.RCCPLexical.showPosition;
import static com.io7m.rocaro.rgraphc.internal.RCCompilerException.exceptionOf;

@JsonPropertyOrder(alphabetic = true)
public final class RCTTypeDeclarationRenderTarget
  extends RCTDeclarationAbstract
  implements RCTTypeDeclarationCompositeType
{
  @JsonProperty("Attachments")
  private final TreeMap<RCCName, RCTAttachmentType> allAttachments;
  private final TreeMap<Integer, RCTColorAttachment> colorAttachmentsByIndex;
  private final TreeMap<RCCName, RCTColorAttachment> colorAttachmentsByName;
  private final SortedMap<RCCName, RCTAttachmentType> allAttachmentsRead;
  private final SortedMap<RCCName, RCTColorAttachment> colorAttachmentsByNameRead;
  private final SortedMap<Integer, RCTColorAttachment> colorAttachmentsByIndexRead;
  private Optional<RCTDepthAttachment> depthAttachment;

  @Override
  public String kind()
  {
    return "RenderTargetType";
  }

  @Override
  public String toString()
  {
    return "[RCTTypeDeclarationRenderTarget %s %s %s %s %s]"
      .formatted(
        this.lexical(),
        this.name(),
        this.colorAttachmentsByNameRead,
        this.colorAttachmentsByIndexRead,
        this.depthAttachment
      );
  }

  private RCTTypeDeclarationRenderTarget(
    final RCTGraphDeclarationType owner,
    final RCCName name)
  {
    super(owner, name);

    this.allAttachments =
      new TreeMap<>();
    this.colorAttachmentsByName =
      new TreeMap<>();
    this.colorAttachmentsByIndex =
      new TreeMap<>();
    this.depthAttachment =
      Optional.empty();

    this.allAttachmentsRead =
      Collections.unmodifiableSortedMap(this.allAttachments);
    this.colorAttachmentsByNameRead =
      Collections.unmodifiableSortedMap(this.colorAttachmentsByName);
    this.colorAttachmentsByIndexRead =
      Collections.unmodifiableSortedMap(this.colorAttachmentsByIndex);
  }

  public static RCTTypeDeclarationRenderTarget.Builder builder(
    final RCTGraphDeclarationType owner,
    final RCCName name)
  {
    return new Builder(owner, name);
  }

  public Optional<RCTDepthAttachment> depthAttachment()
  {
    return this.depthAttachment;
  }

  public SortedMap<Integer, RCTColorAttachment> colorAttachmentsByIndex()
  {
    return this.colorAttachmentsByIndexRead;
  }

  public SortedMap<RCCName, RCTAttachmentType> allAttachments()
  {
    return this.allAttachmentsRead;
  }

  public SortedMap<RCCName, RCTColorAttachment> colorAttachmentsByName()
  {
    return this.colorAttachmentsByNameRead;
  }

  public static final class Builder
  {
    private RCTTypeDeclarationRenderTarget target;

    private Builder(
      final RCTGraphDeclarationType owner,
      final RCCName name)
    {
      this.target =
        new RCTTypeDeclarationRenderTarget(owner, name);
    }

    private static SStructuredError<String> errorDepthAttachmentAlreadySet(
      final LexicalPosition<URI> position,
      final RCCName name)
    {
      return new SStructuredError<>(
        "error-depth-attachment-set",
        "Depth attachment already set.",
        Map.ofEntries(
          Map.entry("Name", name.value()),
          Map.entry("Position (Current)", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    private static SStructuredError<String> errorAttachmentNameUsedWithoutIndex(
      final LexicalPosition<URI> position,
      final RCCName name)
    {
      return new SStructuredError<>(
        "error-attachment-name-used",
        "Attachment name already used.",
        Map.ofEntries(
          Map.entry("Name", name.value()),
          Map.entry("Position (Current)", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    private static SStructuredError<String> errorAttachmentNameUsed(
      final LexicalPosition<URI> position,
      final RCCName name,
      final int index)
    {
      return new SStructuredError<>(
        "error-attachment-name-used",
        "Attachment name already used.",
        Map.ofEntries(
          Map.entry("Name", name.value()),
          Map.entry("Index", Integer.toUnsignedString(index)),
          Map.entry("Position (Current)", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    private static SStructuredError<String> errorAttachmentIndexUsed(
      final LexicalPosition<URI> position,
      final RCCName name,
      final int index)
    {
      return new SStructuredError<>(
        "error-attachment-index-used",
        "Color attachment index already used.",
        Map.ofEntries(
          Map.entry("Name", name.value()),
          Map.entry("Index", Integer.toUnsignedString(index)),
          Map.entry("Position (Current)", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    public RCTTypeDeclarationRenderTarget build()
    {
      this.checkNotBuilt();

      final var r = this.target;
      this.target = null;
      return r;
    }

    public Builder setDepthAttachment(
      final LexicalPosition<URI> position,
      final RCCName name)
      throws RCCompilerException
    {
      Objects.requireNonNull(name, "name");
      this.checkNotBuilt();

      if (this.target.depthAttachment.isPresent()) {
        throw exceptionOf(errorDepthAttachmentAlreadySet(position, name));
      }
      if (this.target.colorAttachmentsByName.containsKey(name)) {
        throw exceptionOf(errorAttachmentNameUsedWithoutIndex(position, name));
      }

      final var attach = new RCTDepthAttachment(this.target, name);
      this.target.depthAttachment = Optional.of(attach);
      this.target.allAttachments.put(name, attach);
      return this;
    }

    public Builder addColorAttachment(
      final LexicalPosition<URI> position,
      final RCCName name,
      final int index)
      throws RCCompilerException
    {
      Objects.requireNonNull(name, "name");
      this.checkNotBuilt();

      if (this.target.colorAttachmentsByIndex.containsKey(index)) {
        throw exceptionOf(errorAttachmentIndexUsed(position, name, index));
      }
      if (this.target.colorAttachmentsByName.containsKey(name)) {
        throw exceptionOf(errorAttachmentNameUsed(position, name, index));
      }
      if (this.target.depthAttachment.isPresent()) {
        final var depth = this.target.depthAttachment.get();
        if (Objects.equals(depth.name(), name)) {
          throw exceptionOf(errorAttachmentNameUsed(position, name, index));
        }
      }

      final var color = new RCTColorAttachment(this.target, name, index);
      this.target.colorAttachmentsByIndex.put(index, color);
      this.target.colorAttachmentsByName.put(name, color);
      this.target.allAttachments.put(name, color);
      return this;
    }

    private void checkNotBuilt()
    {
      if (this.target == null) {
        throw new IllegalStateException("Builder already completed.");
      }
    }
  }
}
