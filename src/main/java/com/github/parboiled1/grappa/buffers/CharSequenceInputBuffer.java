/*
 * Copyright (C) 2014 Francis Galiegue <fgaliegue@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.parboiled1.grappa.buffers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.Futures;
import org.parboiled.support.Chars;
import org.parboiled.support.IndexRange;
import org.parboiled.support.Position;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.nio.CharBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link InputBuffer} over a {@link CharSequence}
 *
 * <p>A {@link CharSequence} is the most basic character interface in the JDK.
 * It is implemented by a lot of character related classes, including {@link
 * String}, and is also the argument type used by a {@link Pattern}'s {@link
 * Matcher}.</p>
 *
 * <p>Among other things, this means you can use this package on very large
 * files using <a href="https://github.com/fge/largetext">largetext</a>, which
 * implements {@link CharSequence} over multi-gigabyte files.</p>
 */
@Immutable
public final class CharSequenceInputBuffer
    implements InputBuffer
{
    private static final ExecutorService EXECUTOR_SERVICE
        = Executors.newCachedThreadPool();
    private final CharSequence charSequence;
    private final Future<LineCounter> lineCounter;

    public CharSequenceInputBuffer(@Nonnull final CharSequence charSequence)
    {
        this.charSequence = Preconditions.checkNotNull(charSequence);
        lineCounter = EXECUTOR_SERVICE.submit(new Callable<LineCounter>()
        {
            @Override
            public LineCounter call()
                throws Exception
            {
                return new LineCounter(charSequence);
            }
        });
    }

    public CharSequenceInputBuffer(@Nonnull final char[] chars)
    {
        this(CharBuffer.wrap(Preconditions.checkNotNull(chars)));
    }

    @Override
    public char charAt(final int index)
    {
        return index >= 0 && index < charSequence.length()
            ? charSequence.charAt(index) : Chars.EOI;
    }

    @Override
    public boolean test(final int index, final char[] characters)
    {
        final int length = characters.length;
        if (index + length > charSequence.length())
            return false;
        for (int i = 0; i < length; i++)
            if (charSequence.charAt(index + i) != characters[i])
                return false;
        return true;
    }

    @Override
    public String extract(final int start, final int end)
    {
        final int realStart = Math.max(start, 0);
        final int realEnd = Math.min(end, charSequence.length());
        return charSequence.subSequence(realStart, realEnd).toString();
    }

    @Override
    public String extract(final IndexRange range)
    {
        return extract(range.start, range.end);
    }

    @Override
    public Position getPosition(final int index)
    {
        return Futures.getUnchecked(lineCounter).toPosition(index);
    }

    @Override
    public String extractLine(final int lineNumber)
    {
        Preconditions.checkArgument(lineNumber > 0, "line number is negative");
        final LineCounter counter = Futures.getUnchecked(lineCounter);
        final Range<Integer> range = counter.getLineRange(lineNumber);
        final int start = range.lowerEndpoint();
        int end = range.upperEndpoint();
        if (charAt(end - 1) == '\n')
            end--;
        if (charAt(end - 1) == '\r')
            end--;
        return extract(start, end);
    }

    @Override
    public int getLineCount()
    {
        return Futures.getUnchecked(lineCounter).getNrLines();
    }

    @Override
    public int length()
    {
        return charSequence.length();
    }
}
