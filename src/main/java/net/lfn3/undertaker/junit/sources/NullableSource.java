package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.Generator;

public interface NullableSource {
    <T> T nullable(Generator<T> generator);
}
