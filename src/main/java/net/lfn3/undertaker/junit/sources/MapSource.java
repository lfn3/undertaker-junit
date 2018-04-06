package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;
import net.lfn3.undertaker.junit.Source;

import java.util.Map;
import java.util.function.BiFunction;

public interface MapSource {
    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, Generator<V> valueGenerator);
    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, Generator<V> valueGenerator, int size);
    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, Generator<V> valueGenerator, int minSize, int maxSize);

    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, BiFunction<Source, K, V> valueGenerator);
    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, BiFunction<Source, K, V> valueGenerator, int size);
    <K, V> Map<K, V> nextMap(Generator<K> keyGenerator, BiFunction<Source, K, V> valueGenerator, int minSize, int maxSize);
}
