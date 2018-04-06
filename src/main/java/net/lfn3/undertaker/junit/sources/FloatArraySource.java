package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToFloatFunction;

public interface FloatArraySource {
    default float[] nextFloatArray()
    {
        return nextFloatArray(FloatSource::nextFloat);
    }
    default float[] nextFloatArray(ToFloatFunction<Source> generator)
    {
        return nextFloatArray(generator, 0, 64);
    }
    default float[] nextFloatArray(ToFloatFunction<Source> generator, int size)
    {
        return nextFloatArray(generator, size, size);
    }
    float[] nextFloatArray(ToFloatFunction<Source> generator, int minSize, int maxSize);
}
