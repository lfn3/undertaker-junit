package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Source;
import net.lfn3.undertaker.junit.primitive.functions.ToCharFunction;

public interface CharArraySource {
    default char[] nextCharArray()
    {
        return nextCharArray(CharSource::nextChar);
    }
    default char[] nextCharArray(ToCharFunction<Source> generator)
    {
        return nextCharArray(generator, 0, 64);
    }
    default char[] nextCharArray(ToCharFunction<Source> generator, int size)
    {
        return nextCharArray(generator, size, size);
    }
    char[] nextCharArray(ToCharFunction<Source> generator, int minSize, int maxSize);
}
