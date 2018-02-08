package net.lfn3.undertaker.junit.primitive.functions;

import java.util.function.Function;

@FunctionalInterface
public interface ToBooleanFunction<T> extends Function<T, Boolean> {
    boolean applyAsBoolean(T value);

    @Override
    default Boolean apply(T t) {
        return applyAsBoolean(t);
    }
}
