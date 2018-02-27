package net.lfn3.undertaker.junit.sources;

import net.lfn3.undertaker.junit.generators.ShortGenerator;

public interface CharSource {
    char getChar();
    char getChar(ShortGenerator codePointSource);
}
