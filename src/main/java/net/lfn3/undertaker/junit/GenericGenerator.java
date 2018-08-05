package net.lfn3.undertaker.junit;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface GenericGenerator<T> extends BiFunction<Source, Class[], T> {
    static <U> GenericGenerator<U> asGenerator(BiFunction<Source, Class[], U> f)
    {
        return f::apply;
    }
}
