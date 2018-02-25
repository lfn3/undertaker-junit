package net.lfn3.undertaker.junit.sources;

public interface ShortSource {
    short getShort();
    short getShort(short max);
    short getShort(short min, short max, short... moreRanges);

    short getShort(int max);
    short getShort(int min, int max, int... moreRanges);
}
