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


package com.io7m.rocaro.rgraphc.internal.loader;

import com.io7m.rocaro.rgraphc.internal.RCCPackageName;
import com.io7m.rocaro.rgraphc.internal.RCCompilerException;
import com.io7m.rocaro.rgraphc.internal.typed.RCTGraphDeclarationType;
import com.io7m.seltzer.api.SStructuredError;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RCCLoader implements RCCLoaderType
{
  public RCCLoader()
  {

  }

  @Override
  public RCTGraphDeclarationType load(
    final RCCPackageName packageName)
    throws RCCompilerException
  {
    final var error =
      new SStructuredError<>(
        "error-package-nonexistent",
        "No such package.",
        Map.ofEntries(
          Map.entry("Package", packageName.value())
        ),
        Optional.empty(),
        Optional.empty()
      );

    throw new RCCompilerException(
      error.message(),
      error.attributes(),
      error.errorCode(),
      error.remediatingAction(),
      List.of(error)
    );
  }
}
