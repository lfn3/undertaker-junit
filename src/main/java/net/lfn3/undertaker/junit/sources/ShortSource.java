package net.lfn3.undertaker.junit.sources;

public interface ShortSource {
    short nextShort();
    short nextShort(short max);
    short nextShort(short min, short max, short... moreRanges);

    short nextShort(int max);
    short nextShort(int min, int max, int... moreRanges);
}
