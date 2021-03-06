package com.github.fge.grappa.matchers.repeat;

import com.github.fge.grappa.rules.Rule;

public final class ExactMatchesRepeatMatcher
    extends RepeatMatcher
{
    private final int nrCycles;

    public ExactMatchesRepeatMatcher(final Rule subRule, final int nrCycles)
    {
        super(subRule);
        this.nrCycles = nrCycles;
    }

    @Override
    protected boolean enoughCycles(final int cycles)
    {
        return cycles == nrCycles;
    }

    @Override
    protected boolean runAgain(final int cycles)
    {
        return cycles < nrCycles;
    }
}
