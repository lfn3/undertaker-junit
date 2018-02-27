package net.lfn3.undertaker.junit.generators;

import net.lfn3.undertaker.junit.sources.ShortSource;

public class CodePoints {
    public static final ShortGenerator ANY = ShortSource::getShort;
    public static final ShortGenerator ASCII = source -> source.getShort(32, 126);
    public static final ShortGenerator ALPHANUMERIC = source -> source.getShort(48, 57, 65, 90, 97,122);
    public static final ShortGenerator ALPHA = source -> source.getShort(65, 90, 97,122);
    public static final ShortGenerator DIGITS = source -> source.getShort(48, 57);
}
