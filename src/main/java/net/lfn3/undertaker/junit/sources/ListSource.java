package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;

import java.util.List;

public interface ListSource
{
    default <T> List<T> nextList(Generator<T> generator) {
        return nextList(generator, 0, 64);
    }

    default <T> List<T> nextList(Generator<T> generator, int size) {
        return nextList(generator, size, size);
    }

    <T> List<T> nextList(Generator<T> generator, int min, int max);
}
