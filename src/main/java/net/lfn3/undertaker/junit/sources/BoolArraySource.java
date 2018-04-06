package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToBooleanFunction;

public interface BoolArraySource {
    default boolean[] nextBooleanArray()
    {
        return nextBooleanArray(BoolSource::nextBool);
    }
    default boolean[] nextBooleanArray(ToBooleanFunction<Source> generator)
    {
        return nextBooleanArray(generator, 0, 64);
    }
    default boolean[] nextBooleanArray(ToBooleanFunction<Source> generator, int size)
    {
        return nextBooleanArray(generator, size, size);
    }
    boolean[] nextBooleanArray(ToBooleanFunction<Source> generator, int minSize, int maxSize);
}
