package net.lfn3.undertaker.junit.primitive.functions;

import java.util.function.Function;

@FunctionalInterface
public interface ToByteFunction<T> extends Function<T, Byte> {
    byte applyAsByte(T value);

    @Override
    default Byte apply(T t) {
        return applyAsByte(t);
    }
}
