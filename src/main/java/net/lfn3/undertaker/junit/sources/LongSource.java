package net.lfn3.undertaker.junit.sources;

public interface LongSource {
    default long nextLong()
    {
        return nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    default long nextLong(long max)
    {
        return nextLong(Long.MIN_VALUE, max);
    }

    long nextLong(long min, long max);
}
