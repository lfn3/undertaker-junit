package net.lfn3.undertaker.junit.primitive.functions;

import java.util.function.Function;

@FunctionalInterface
public interface ToCharFunction<T> extends Function<T, Character> {
    char applyAsChar(T value);

    @Override
    default Character apply(T t) {
        return applyAsChar(t);
    }
}
