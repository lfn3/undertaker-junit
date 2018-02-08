package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToBooleanFunction;

public interface BoolArraySource {
    default boolean[] getBooleanArray()
    {
        return getBooleanArray(BoolSource::getBool);
    }
    default boolean[] getBooleanArray(ToBooleanFunction<Source> generator)
    {
        return getBooleanArray(generator, 0, 64);
    }
    default boolean[] getBooleanArray(ToBooleanFunction<Source> generator, int size)
    {
        return getBooleanArray(generator, size, size);
    }
    boolean[] getBooleanArray(ToBooleanFunction<Source> generator, int minSize, int maxSize);
}
