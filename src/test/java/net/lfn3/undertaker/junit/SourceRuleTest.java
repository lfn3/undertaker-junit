package net.lfn3.undertaker.junit;

import net.lfn3.undertaker.junit.generators.CodePoints;
import net.lfn3.undertaker.junit.primitive.functions.ToByteFunction;
import net.lfn3.undertaker.junit.sources.ByteSource;
import net.lfn3.undertaker.junit.sources.IntArraySource;
import net.lfn3.undertaker.junit.sources.IntSource;
import net.lfn3.undertaker.junit.sources.StringSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class SourceRuleTest {
    private static final Generator<Date> DATE_GENERATOR = s -> Date.from(Instant.ofEpochSecond(s.getInt(0, Integer.MAX_VALUE)));
    private static final Generator<String> DATE_STRING_GENERATOR = Generator.asGenerator(DATE_GENERATOR.andThen(Date::toString));

    private int clearedBefore;
    private List<Long> aList = new ArrayList<>();
    private static final Map<Class, Generator> GENERATORS = new HashMap<>();

    static {
        GENERATORS.put(GeneratorMapTestClass.class, s -> new GeneratorMapTestClass("Hello!"));
    }

    @Rule
    public Source source = new SourceRule(GENERATORS);

    @Before
    public void before() {
        clearedBefore = 0;
    }

    @Test
    public void beforeRunsWithEveryIteration() {
        Assert.assertEquals(0, clearedBefore);
        clearedBefore = source.getInt();
    }

    @Test
    public void runsOnANewlyInstansiatedClass() {
        Assert.assertEquals(0, aList.size());
        aList.add(source.getLong());
    }

    @Test
    public void compilesAndRuns() {
        Assert.assertTrue(true);
    }

    @Test
    public void canGetAnInt() {
        int anInt = source.getInt();
        Assert.assertTrue(anInt >= Integer.MIN_VALUE);
        Assert.assertTrue(anInt <= Integer.MAX_VALUE);
    }

    @Test
    public void canGetIntInRange() {
        int anInt = source.getInt(1, 10);

        Assert.assertTrue(anInt >= 1);
        Assert.assertTrue(anInt <= 10);
    }

    @Test
    public void canGetAnBetweenMaxAndMin() {
        int anInt = source.getInt(0, 1);
        Assert.assertTrue(anInt == 0 || anInt == 1);
    }

    @Test(expected = AssertionError.class)
    public void canFail() {
        Assert.assertTrue(false);
    }

    @Test(expected = AssertionError.class)
    public void canFailWithNiceishMessageWhenUsingAGenerator() {
        Assert.assertNull(source.getBool());
    }

    @Test
    public void canGetABoolean() {
        final boolean bool = source.getBool();
        Assert.assertTrue(bool || !bool);
    }

    @Test
    public void canGetAByte() {
        final byte aByte = source.getByte();
        Assert.assertTrue(aByte >= Byte.MIN_VALUE);
        Assert.assertTrue(aByte <= Byte.MAX_VALUE);
    }

    @Test
    public void canGetAList() {
        final List<Date> list = source.getList(SourceRuleTest::generateDate);
        Assert.assertNotNull(list);

        final List<GeneratorMapTestClass> fixedSize = source.getList(s -> s.generate(GeneratorMapTestClass.class), 5);
        Assert.assertTrue(fixedSize.size() == 5);

        final List<Byte> aListAOfBytes = source.getList(ByteSource::getByte, 1, 10);
        Assert.assertNotNull(aListAOfBytes);
        Assert.assertTrue(1 <= aListAOfBytes.size());
        Assert.assertTrue(aListAOfBytes.size() <= 10);
    }

    @Test
    public void canGetAnArray() {
        final Date[] anArray = source.getArray(Date.class, SourceRuleTest::generateDate);
        Assert.assertNotNull(anArray);

        final GeneratorMapTestClass[] fixedSize = source.getArray(
                GeneratorMapTestClass.class, s -> s.generate(GeneratorMapTestClass.class), 5);
        Assert.assertTrue(fixedSize.length == 5);

        ToByteFunction<Source> getByte = Source::getByte;
        final byte[] aByteArray = source.getByteArray(getByte, 1, 10);
        Assert.assertNotNull(aByteArray);

        final byte[] byteArrayWithGenerator = source.getByteArray(Source::getByte);
        Assert.assertNotNull(byteArrayWithGenerator);

        final int[] anIntArray = source.getIntArray(Source::getInt);
        Assert.assertNotNull(anIntArray);
        Assert.assertTrue(1 <= aByteArray.length);
        Assert.assertTrue(aByteArray.length <= 10);
    }

    @Test
    public void canGenerateWithFunction() {
        final Date generated = source.generate(SourceRuleTest::generateDate);
        Assert.assertNotNull(generated);

        final Date functionGenerated = source.generate(DATE_GENERATOR);
        Assert.assertNotNull(functionGenerated);

        final String composedFunctionGenerated = source.generate(DATE_STRING_GENERATOR);
        Assert.assertNotNull(composedFunctionGenerated);
    }

    @Test
    public void canGetAShort() {
        final short aShort = source.getShort();
        Assert.assertTrue(aShort >= Short.MIN_VALUE);
        Assert.assertTrue(aShort <= Short.MAX_VALUE);
    }

    @Test
    public void canGetAFloat() {
        final float aFloat = source.getFloat();
        if (Double.isFinite(aFloat) && !Double.isNaN(aFloat)) {
            Assert.assertTrue(aFloat >= -Float.MAX_VALUE);
            Assert.assertTrue(aFloat <= Float.MAX_VALUE);
        }
    }

    @Test
    public void canGetADouble() {
        final double aDouble = source.getDouble();
        if (Double.isFinite(aDouble) && !Double.isNaN(aDouble)) {
            Assert.assertTrue(aDouble >= -Double.MAX_VALUE);
            Assert.assertTrue(aDouble <= Double.MAX_VALUE);
        }
    }

    @Test
    public void canGetRealDouble() {
        final double realDouble = source.getRealDouble();

        Assert.assertFalse(Double.isInfinite(realDouble));
        Assert.assertFalse(Double.isNaN(realDouble));

        Assert.assertTrue(realDouble >= -Double.MAX_VALUE);
        Assert.assertTrue(realDouble <= Double.MAX_VALUE);
    }

    @Test
    public void canGetALong() {
        final long aLong = source.getLong();
        Assert.assertTrue(aLong >= Long.MIN_VALUE);
        Assert.assertTrue(aLong <= Long.MAX_VALUE);
    }

    @Test
    public void canGetLongArray() {
        final long[] someLongs = source.getLongArray();
        for (long aLong : someLongs) {
            Assert.assertTrue(aLong >= Long.MIN_VALUE);
            Assert.assertTrue(aLong <= Long.MAX_VALUE);
        }
    }

    @Test
    public void canGetEveryKindOfChar() {
        final char c = source.getChar();
        final char ascii = source.getAsciiChar();

        final char alpha = source.getAlphaChar();
        Assert.assertTrue(Character.isAlphabetic(alpha));

        final char alphaNum = source.getAlphanumericChar();
        Assert.assertTrue(Character.isAlphabetic(alphaNum) || Character.isDigit(alphaNum));
    }

    @Test
    public void canGetEveryKindOfString() {
        final String s = source.getString();
        Assert.assertTrue(s != null);
        final String ascii = source.getString(CodePoints.ASCII);
        Assert.assertTrue(ascii != null);

        final String alpha = source.getString(CodePoints.ALPHA);
        for (char c : alpha.toCharArray()) {
            Assert.assertTrue(Character.isAlphabetic(c));
        }

        final String alphaNum = source.getString(CodePoints.ALPHANUMERIC);
        for (char c : alpha.toCharArray()) {
            Assert.assertTrue(Character.isAlphabetic(c) || Character.isDigit(c));
        }

        final String fixedSize = source.getString(CodePoints.ASCII, 5);
        Assert.assertTrue(fixedSize.length() == 5);
    }

    private enum AnEnum {A, B, C}

    @Test
    public void canGetAnEnum() {
        AnEnum anEnum = source.getEnum(AnEnum.class);

        Assert.assertTrue(anEnum == AnEnum.A || anEnum == AnEnum.B || anEnum == AnEnum.C);
    }

    @Test
    public void canReflectivelyGetAnEnum() {
        AnEnum anEnum = source.reflectively(AnEnum.class);

        Assert.assertTrue(anEnum == AnEnum.A || anEnum == AnEnum.B || anEnum == AnEnum.C);
    }

    @Test
    public void canGetFromCollection() {
        AnEnum anEnum = source.from(AnEnum.values());
        Assert.assertTrue(anEnum == AnEnum.A || anEnum == AnEnum.B || anEnum == AnEnum.C);

        Integer i = source.from(Arrays.asList(1, 2, 3));
        Assert.assertTrue(i <= 3);
        Assert.assertTrue(1 <= i);
    }

    @Test
    public void canGetAMap() {
        final Map<String, String> m = source.getMap(
                s -> s.getString(CodePoints.ALPHANUMERIC),
                s -> s.getString(CodePoints.ALPHA));

        m.keySet().forEach(s -> {
            for (char c : s.toCharArray()) {
                Assert.assertTrue(Character.isAlphabetic(c) || Character.isDigit(c));
            }
        });

        m.values().forEach(s -> {
            for (char c : s.toCharArray()) {
                Assert.assertTrue(Character.isAlphabetic(c));
            }
        });

        final Map<Integer, Character> fixedSize = source.getMap(Source::getInt, Source::getChar, 5);
        Assert.assertTrue(fixedSize.size() == 5);


        final Map<Integer, Character> sized = source.getMap(Source::getInt, Source::getChar, 5, 10);
        Assert.assertTrue(5 <= sized.size());
        Assert.assertTrue(sized.size() <= 10);
    }

    @Test
    public void canGetAMapWhereTheValueGenTakesKeyAsAnArg() {
        final Map<String, String> m = source.getMap(
                s -> s.getString(CodePoints.ALPHANUMERIC),
                (s, k) -> k + s.getString(CodePoints.ALPHA));

        m.keySet().forEach(key -> {
            for (char c : key.toCharArray()) {
                Assert.assertTrue(Character.isAlphabetic(c) || Character.isDigit(c));
            }
            final String value = m.get(key);
            Assert.assertTrue(value.startsWith(key));
            for (char c : value.substring(key.length()).toCharArray()) {
                Assert.assertTrue(Character.isAlphabetic(c));
            }
        });
    }

    @Test
    public void canUseGeneratorsFromSource() {
        GeneratorMapTestClass generated = source.generate(GeneratorMapTestClass.class);
        Assert.assertNotNull(generated);
    }

    @Test
    @Seed(1234567)
    @Trials(1)
    public void annotationsWork() {
        Assert.assertEquals(-1921583219793701470L, source.getLong());
    }

    @Test
    public void reflectiveOverPrimitives() {
        final Long aLong = source.reflectively(Long.class);
        Assert.assertNotNull(aLong);
        final long anotherLong = source.reflectively(long.class);
        final Boolean aBool = source.reflectively(Boolean.class);
        Assert.assertNotNull(aBool);
        final boolean anotherBool = source.reflectively(boolean.class);
    }

    @Test
    public void reflectiveApi() throws Exception {
        final Date aDate = source.reflectively(Date.class.getConstructor(long.class));
        Assert.assertNotNull(aDate);

        final ClassWithStaticConstructor aClass =
                source.generate(s -> ClassWithStaticConstructor.constructor(s.reflectively(ClassWithConstructor.class)));
        Assert.assertNotNull(aClass);
    }

    @Test
    public void canGetSet() {
        final Set<Integer> set = source.getSet(IntSource::getInt);
        Assert.assertNotNull(set);

        set.forEach(anInt -> {
            Assert.assertTrue(anInt >= Integer.MIN_VALUE);
            Assert.assertTrue(anInt <= Integer.MAX_VALUE);
        });

        final Set<String> fixedSize = source.getSet(Source::getString, 5);
        Assert.assertTrue(fixedSize.size() == 5);

        final Set<String> sized = source.getSet(Source::getString, 5, 10);
        Assert.assertTrue(5 <= sized.size());
        Assert.assertTrue(sized.size() <= 10);
    }

    @Test
    public void canGetNullable() {
        final String nullableString = source.getNullable(StringSource::getString);
        Assert.assertTrue(nullableString == null || nullableString != null);
    }

    @Test
    public void canGet2DArray() throws Exception {
        final int[][] array = source.getArray(int[].class, IntArraySource::getIntArray);
        Assert.assertNotNull(array);

        final int[][] anotherArray = source.getArray(int[].class, s -> s.getIntArray(Source::getInt));
        Assert.assertNotNull(anotherArray);
    }

    public static Date generateDate(Source s) {
        s.pushInterval();
        final Date generatedValue = Date.from(Instant.ofEpochSecond(s.getInt()));
        s.popInterval(generatedValue);

        return generatedValue;
    }

    public static class GeneratorMapTestClass {
        public final String s;

        public GeneratorMapTestClass(String s) {
            this.s = s;
        }
    }

    public static class ClassWithConstructor {
        public ClassWithConstructor(GeneratorMapTestClass c) {
        }
    }

    public static class ClassWithStaticConstructor {
        public static ClassWithStaticConstructor constructor(ClassWithConstructor c) {
            return new ClassWithStaticConstructor();
        }
    }
}
