package net.lfn3.undertaker.junit.generators;

import net.lfn3.undertaker.junit.sources.ShortSource;

public class CodePoints {
    public static final ShortGenerator ANY = ShortSource::nextShort;
    public static final ShortGenerator ASCII = source -> source.nextShort(32, 126);
    public static final ShortGenerator ALPHANUMERIC = source -> source.nextShort(48, 57, 65, 90, 97,122);
    public static final ShortGenerator ALPHA = source -> source.nextShort(65, 90, 97,122);
    public static final ShortGenerator DIGITS = source -> source.nextShort(48, 57);
}
