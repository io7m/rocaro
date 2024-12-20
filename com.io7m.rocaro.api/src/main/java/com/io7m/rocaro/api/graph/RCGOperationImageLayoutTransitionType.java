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


package com.io7m.rocaro.api.graph;

import java.util.Objects;
import java.util.Optional;

/**
 * The type of image layout transitions that may occur for operations.
 */

public sealed interface RCGOperationImageLayoutTransitionType
{
  /**
   * @return The pre-operation layout transition, if any
   */

  Optional<Pre> pre();

  /**
   * @return The post-operation layout transition, if any
   */

  Optional<Post> post();

  /**
   * An operation holds an image layout constant.
   *
   * @param layout The layout
   */

  record Constant(
    RCGResourceImageLayout layout)
    implements RCGOperationImageLayoutTransitionType
  {
    /**
     * An operation holds an image layout constant.
     */

    public Constant
    {
      Objects.requireNonNull(layout, "layout");
    }

    @Override
    public Optional<Pre> pre()
    {
      return Optional.empty();
    }

    @Override
    public Optional<Post> post()
    {
      return Optional.empty();
    }
  }

  /**
   * An operation performs an image layout transition just prior to executing.
   *
   * @param layoutFrom The source layout
   * @param layoutTo   The target layout
   */

  record Pre(
    RCGResourceImageLayout layoutFrom,
    RCGResourceImageLayout layoutTo)
    implements RCGOperationImageLayoutTransitionType
  {
    /**
     * An operation performs an image layout transition just prior to executing.
     */

    public Pre
    {
      Objects.requireNonNull(layoutFrom, "layoutFrom");
      Objects.requireNonNull(layoutTo, "layoutTo");
    }

    @Override
    public Optional<Pre> pre()
    {
      return Optional.of(this);
    }

    @Override
    public Optional<Post> post()
    {
      return Optional.empty();
    }
  }

  /**
   * An operation performs an image layout transition just after executing.
   *
   * @param layoutFrom The source layout
   * @param layoutTo   The target layout
   */

  record Post(
    RCGResourceImageLayout layoutFrom,
    RCGResourceImageLayout layoutTo)
    implements RCGOperationImageLayoutTransitionType
  {
    /**
     * An operation performs an image layout transition just after executing.
     */

    public Post
    {
      Objects.requireNonNull(layoutFrom, "layoutFrom");
      Objects.requireNonNull(layoutTo, "layoutTo");
    }

    @Override
    public Optional<Pre> pre()
    {
      return Optional.empty();
    }

    @Override
    public Optional<Post> post()
    {
      return Optional.of(this);
    }
  }

  /**
   * An operation performs image layout transitions before and after executing.
   *
   * @param layoutFrom   The pre-operation layout transition
   * @param layoutDuring The during-execution layout
   * @param layoutTo     The post-operation layout transition
   */

  record PreAndPost(
    RCGResourceImageLayout layoutFrom,
    RCGResourceImageLayout layoutDuring,
    RCGResourceImageLayout layoutTo)
    implements RCGOperationImageLayoutTransitionType
  {
    /**
     * An operation performs image layout transitions before and after executing.
     */

    public PreAndPost
    {
      Objects.requireNonNull(layoutFrom, "layoutFrom");
      Objects.requireNonNull(layoutDuring, "layoutDuring");
      Objects.requireNonNull(layoutTo, "layoutTo");
    }

    @Override
    public Optional<Pre> pre()
    {
      return Optional.of(new Pre(this.layoutFrom, this.layoutDuring));
    }

    @Override
    public Optional<Post> post()
    {
      return Optional.of(new Post(this.layoutDuring, this.layoutTo));
    }
  }
}
