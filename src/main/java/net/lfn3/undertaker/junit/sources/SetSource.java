package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;

import java.util.Set;

public interface SetSource {
    <V> Set<V> nextSet(Generator<V> generator);
    <V> Set<V> nextSet(Generator<V> generator, int size);
    <V> Set<V> nextSet(Generator<V> generator, int minSize, int maxSize);
}
