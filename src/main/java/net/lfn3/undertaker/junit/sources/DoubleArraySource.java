package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;

import java.util.function.ToDoubleFunction;

public interface DoubleArraySource {
    default double[] getDoubleArray()
    {
        return getDoubleArray(DoubleSource::getDouble);
    }
    default double[] getDoubleArray(ToDoubleFunction<Source> generator)
    {
        return getDoubleArray(generator, 0, 64);
    }
    default double[] getDoubleArray(ToDoubleFunction<Source> generator, int size)
    {
        return getDoubleArray(generator, size, size);
    }
    double[] getDoubleArray(ToDoubleFunction<Source> generator, int minSize, int maxSize);
}
