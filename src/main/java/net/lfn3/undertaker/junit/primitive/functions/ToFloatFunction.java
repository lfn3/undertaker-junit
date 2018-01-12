package net.lfn3.undertaker.junit.primitive.functions;

import java.util.function.Function;

@FunctionalInterface
public interface ToFloatFunction<T> extends Function<T, Float> {
    float applyAsFloat(T value);

    @Override
    default Float apply(T t) {
        return applyAsFloat(t);
    }
}
