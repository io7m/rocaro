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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.io7m.rocaro.rgraphc.internal.RCCPLexical.showPosition;
import static com.io7m.rocaro.rgraphc.internal.RCCompilerException.exceptionOf;

@JsonPropertyOrder(alphabetic = true)
public final class RCTTypeDeclarationRecord
  extends RCTDeclarationAbstract
  implements RCTTypeDeclarationCompositeType
{
  private final TreeMap<RCCName, RCTField> fields;
  private final SortedMap<RCCName, RCTField> fieldsRead;

  private RCTTypeDeclarationRecord(
    final RCTGraphDeclarationType owner,
    final RCCName name)
  {
    super(owner, name);

    this.fields =
      new TreeMap<>();
    this.fieldsRead =
      Collections.unmodifiableSortedMap(this.fields);
  }

  public static RCTTypeDeclarationRecord.Builder builder(
    final RCTGraphDeclarationType owner,
    final RCCName name)
  {
    return new Builder(owner, name);
  }

  @Override
  public String toString()
  {
    return "[RCTTypeDeclarationRecord %s %s %s]"
      .formatted(
        this.lexical(),
        this.name(),
        this.fields
      );
  }

  @JsonProperty("Fields")
  public SortedMap<RCCName, RCTField> fields()
  {
    return this.fieldsRead;
  }

  @Override
  public String kind()
  {
    return "RecordType";
  }

  public static final class Builder
  {
    private RCTTypeDeclarationRecord target;

    private Builder(
      final RCTGraphDeclarationType owner,
      final RCCName name)
    {
      this.target =
        new RCTTypeDeclarationRecord(owner, name);
    }

    private static SStructuredError<String> errorFieldNameUsed(
      final LexicalPosition<URI> position,
      final RCCName name)
    {
      return new SStructuredError<>(
        "error-field-name-used",
        "Field name already used.",
        Map.ofEntries(
          Map.entry("Name", name.value()),
          Map.entry("Position (Current)", showPosition(position))
        ),
        Optional.empty(),
        Optional.empty()
      );
    }

    public RCTTypeDeclarationRecord build()
    {
      this.checkNotBuilt();

      final var r = this.target;
      this.target = null;
      return r;
    }

    public Builder addField(
      final LexicalPosition<URI> position,
      final RCCName name,
      final RCTTypeDeclarationType type)
      throws RCCompilerException
    {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(type, "type");

      this.checkNotBuilt();

      if (this.target.fields.containsKey(name)) {
        throw exceptionOf(errorFieldNameUsed(position, name));
      }

      final var field = new RCTField(this.target, name, type);
      this.target.fields.put(name, field);
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
