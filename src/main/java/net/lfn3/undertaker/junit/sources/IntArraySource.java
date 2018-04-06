package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;

import java.util.function.ToIntFunction;

public interface IntArraySource {
    default int[] nextIntArray()
    {
        return nextIntArray(IntSource::nextInt);
    }
    default int[] nextIntArray(ToIntFunction<Source> generator)
    {
        return nextIntArray(generator, 0, 64);
    }
    default int[] nextIntArray(ToIntFunction<Source> generator, int size)
    {
        return nextIntArray(generator, size, size);
    }
    int[] nextIntArray(ToIntFunction<Source> generator, int minSize, int maxSize);
}
