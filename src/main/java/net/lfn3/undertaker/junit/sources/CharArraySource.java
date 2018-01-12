package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToCharFunction;

public interface CharArraySource {
    default char[] getCharArray()
    {
        return getCharArray(CharSource::getChar);
    }
    default char[] getCharArray(ToCharFunction<Source> generator)
    {
        return getCharArray(generator, 0, 64);
    }
    default char[] getCharArray(ToCharFunction<Source> generator, int size)
    {
        return getCharArray(generator, size, size);
    }
    char[] getCharArray(ToCharFunction<Source> generator, int minSize, int maxSize);
}
