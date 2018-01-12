package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToBoolFunction;

public interface BoolArraySource {
    default boolean[] getBooleanArray()
    {
        return getBooleanArray(BoolSource::getBool);
    }
    default boolean[] getBooleanArray(ToBoolFunction<Source> generator)
    {
        return getBooleanArray(generator, 0, 64);
    }
    default boolean[] getBooleanArray(ToBoolFunction<Source> generator, int size)
    {
        return getBooleanArray(generator, size, size);
    }
    boolean[] getBooleanArray(ToBoolFunction<Source> generator, int minSize, int maxSize);
}
