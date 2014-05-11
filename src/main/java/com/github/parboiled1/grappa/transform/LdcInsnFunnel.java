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

package com.github.parboiled1.grappa.transform;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * A {@link Funnel} for an LDC instruction
 *
 * @see InstructionGroupHasher#visitLdcInsn(Object)
 */
@Immutable
@ParametersAreNonnullByDefault
public enum LdcInsnFunnel
    implements Funnel<Object>
{
    INSTANCE
    {
        /**
         * Sends a stream of data from the {@code from} object into the
         * sink {@code into}. There
         * is no requirement that this data be complete enough to fully
         * reconstitute the object
         * later.
         *
         * @param from
         * @param into
         * @since 12.0 (in Guava 11.0, {@code PrimitiveSink} was named
         * {@code Sink})
         */
        @Override
        public void funnel(final Object from, final PrimitiveSink into)
        {
            for (final Map.Entry<Class<?>, Funnel<Object>> entry:
                FUNNELS.entrySet())
                if (Predicates.instanceOf(entry.getKey()).apply(from)) {
                    entry.getValue().funnel(from, into);
                    return;
                }

            throw new UnsupportedOperationException("unsupported class "
                + from.getClass().getCanonicalName());
        }
    };

    private static final Map<Class<?>, Funnel<Object>> FUNNELS;

    static {
        final ImmutableMap.Builder<Class<?>, Funnel<Object>> builder
            = ImmutableMap.builder();

        builder.put(Integer.class, integerFunnel())
            .put(Float.class, floatFunnel())
            .put(Long.class, longFunnel())
            .put(Double.class, doubleFunnel())
            .put(String.class, stringFunnel())
            .put(Type.class, asmTypeFunnel());

        FUNNELS = builder.build();
    }

    private static Funnel<Object> integerFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                into.putInt((Integer) from);
            }
        };
    }

    private static Funnel<Object> floatFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                into.putFloat((Float) from);
            }
        };
    }

    private static Funnel<Object> longFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                into.putLong((Long) from);
            }
        };
    }

    private static Funnel<Object> doubleFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                into.putDouble((Double) from);
            }
        };
    }

    private static Funnel<Object> stringFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                into.putUnencodedChars((CharSequence) from);
            }
        };
    }

    private static Funnel<Object> asmTypeFunnel()
    {
        return new Funnel<Object>()
        {
            @Override
            public void funnel(final Object from, final PrimitiveSink into)
            {
                final Type type = (Type) from;
                into.putUnencodedChars(type.getInternalName());
            }
        };
    }
}