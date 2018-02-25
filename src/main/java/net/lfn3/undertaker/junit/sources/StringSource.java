package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.generators.ShortGenerator;

public interface StringSource {
    String getString();
    String getString(ShortGenerator codePointGenerator);
    String getString(ShortGenerator codePointGenerator, int size);
    String getString(ShortGenerator codePointGenerator, int minLength, int maxLength);
}
