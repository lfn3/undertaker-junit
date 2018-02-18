package net.lfn3.undertaker.junit.sources;

import java.math.BigInteger;

public interface BigIntegerSource {
    BigInteger getBigInteger();
    BigInteger getBigInteger(BigInteger max);
    BigInteger getBigInteger(BigInteger min, BigInteger max);
}
