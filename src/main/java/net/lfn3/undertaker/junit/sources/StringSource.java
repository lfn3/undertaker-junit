package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.generators.ShortGenerator;

public interface StringSource {
    String nextString();
    String nextString(ShortGenerator codePointGenerator);
    String nextString(ShortGenerator codePointGenerator, int size);
    String nextString(ShortGenerator codePointGenerator, int minLength, int maxLength);
}
