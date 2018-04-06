package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToByteFunction;

public interface ByteArraySource {
    default byte[] nextByteArray()
    {
        return nextByteArray(ByteSource::nextByte);
    }
    default byte[] nextByteArray(ToByteFunction<Source> generator)
    {
        return nextByteArray(generator, 0, 64);
    }
    default byte[] nextByteArray(ToByteFunction<Source> generator, int size)
    {
        return nextByteArray(generator, size, size);
    }
    byte[] nextByteArray(ToByteFunction<Source> generator, int minSize, int maxSize);
}
