package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToFloatFunction;

public interface FloatArraySource {
    default float[] getFloatArray()
    {
        return getFloatArray(FloatSource::getFloat);
    }
    default float[] getFloatArray(ToFloatFunction<Source> generator)
    {
        return getFloatArray(generator, 0, 64);
    }
    default float[] getFloatArray(ToFloatFunction<Source> generator, int size)
    {
        return getFloatArray(generator, size, size);
    }
    float[] getFloatArray(ToFloatFunction<Source> generator, int minSize, int maxSize);
}
