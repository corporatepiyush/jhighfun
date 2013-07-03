package org.jhighfun.util.matcher;


import org.jhighfun.util.Function;

public interface WhenFunctionExecutor<IN, OUT> {

    public ThenFunctionExecutor<IN, OUT> when(final IN matchingInput);

    public ThenFunctionExecutor<IN, OUT> when(Function<IN, Boolean> condition);

    public OUT otherwise(OUT outputObject);

    public OUT otherwise(Function<IN, OUT> function);
}
