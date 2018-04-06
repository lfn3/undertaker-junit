package net.lfn3.undertaker.junit.sources;

public interface IntSource {
    default int nextInt()
    {
        return nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    default int nextInt(int max)
    {
        return nextInt(Integer.MIN_VALUE, max);
    }

    int nextInt(int min, int max);

    int nextInt(int min, int max, int... moreRanges);
}
