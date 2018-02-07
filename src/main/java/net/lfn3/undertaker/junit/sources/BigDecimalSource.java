package net.lfn3.undertaker.junit.sources;

import java.math.BigDecimal;

public interface BigDecimalSource {
    BigDecimal getBigDecimal();
    BigDecimal getBigDecimal(BigDecimal max);
    BigDecimal getBigDecimal(BigDecimal min, BigDecimal max);
}
