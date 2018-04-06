package net.lfn3.undertaker.junit.sources;

public interface ByteSource {
    default byte nextByte()
    {
        return nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    default byte nextByte(byte max)
    {
        return nextByte(Byte.MIN_VALUE, max);
    }

    byte nextByte(byte min, byte max);

    byte nextByte(int max);

    byte nextByte(int min, int max);
}
