package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;

import java.util.function.ToDoubleFunction;

public interface DoubleArraySource {
    default double[] nextDoubleArray()
    {
        return nextDoubleArray(DoubleSource::nextDouble);
    }
    default double[] nextDoubleArray(ToDoubleFunction<Source> generator)
    {
        return nextDoubleArray(generator, 0, 64);
    }
    default double[] nextDoubleArray(ToDoubleFunction<Source> generator, int size)
    {
        return nextDoubleArray(generator, size, size);
    }
    double[] nextDoubleArray(ToDoubleFunction<Source> generator, int minSize, int maxSize);
}
