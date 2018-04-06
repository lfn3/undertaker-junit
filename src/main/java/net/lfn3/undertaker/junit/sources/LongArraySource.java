package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;

import java.util.function.ToLongFunction;

public interface LongArraySource {
    default long[] nextLongArray()
    {
        return nextLongArray(LongSource::nextLong);
    }
    default long[] nextLongArray(ToLongFunction<Source> generator)
    {
        return nextLongArray(generator, 0, 64);
    }
    default long[] nextLongArray(ToLongFunction<Source> generator, int size)
    {
        return nextLongArray(generator, size, size);
    }
    long[] nextLongArray(ToLongFunction<Source> generator, int minSize, int maxSize);
}
