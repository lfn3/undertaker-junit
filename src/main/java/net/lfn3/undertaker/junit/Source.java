package net.lfn3.undertaker.junit;

import net.lfn3.undertaker.junit.sources.*;
import org.junit.rules.TestRule;

public interface Source extends BoolSource,
                                BoolArraySource,
                                ByteSource,
                                ByteArraySource,
                                ShortSource,
                                ShortArraySource,
                                IntSource,
                                IntArraySource,
                                CharSource,
                                CharArraySource,
                                LongSource,
                                LongArraySource,
                                FloatSource,
                                FloatArraySource,
                                DoubleSource,
                                DoubleArraySource,
                                StringSource,
                                ListSource,
                                ArraySource,
                                ObjectSource,
                                MapSource,
                                SetSource,
                                EnumSource,
                                FromCollectionSource,
                                ReflectiveSource,
                                NullableSource,
                                TestRule {
    void pushInterval();

    void popInterval(Object generatedValue);
}
