package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;

import java.util.function.ToLongFunction;

public interface LongArraySource {
    default long[] getLongArray()
    {
        return getLongArray(LongSource::getLong);
    }
    default long[] getLongArray(ToLongFunction<Source> generator)
    {
        return getLongArray(generator, 0, 64);
    }
    default long[] getLongArray(ToLongFunction<Source> generator, int size)
    {
        return getLongArray(generator, size, size);
    }
    long[] getLongArray(ToLongFunction<Source> generator, int minSize, int maxSize);
}
