package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToShortFunction;

public interface ShortArraySource {
    default short[] nextShortArray()
    {
        return nextShortArray(ShortSource::nextShort);
    }
    default short[] nextShortArray(ToShortFunction<Source> generator)
    {
        return nextShortArray(generator, 0, 64);
    }
    default short[] nextShortArray(ToShortFunction<Source> generator, int size)
    {
        return nextShortArray(generator, size, size);
    }
    short[] nextShortArray(ToShortFunction<Source> generator, int minSize, int maxSize);
}
