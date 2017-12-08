package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;

import java.util.Set;

public interface SetSource {
    <V> Set<V> getSet(Generator<V> generator);
}
