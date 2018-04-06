package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;

public interface ArraySource {
    default <T> T[] nextArray(Class<T> klass, Generator<T> generator)
    {
        return nextArray(klass, generator, 0, 64);
    }

    default <T> T[] nextArray(Class<T> klass, Generator<T> generator, int size)
    {
        return nextArray(klass, generator, size, size);
    }

    <T> T[] nextArray(Class<T> klass, Generator<T> generator, int min, int max);
}
