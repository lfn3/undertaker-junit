package net.lfn3.undertaker.junit;

import net.lfn3.undertaker.junit.generators.CodePoints;
import net.lfn3.undertaker.junit.primitive.functions.ToByteFunction;
import net.lfn3.undertaker.junit.sources.*;
import org.junit.*;

import javax.swing.text.html.Option;
import java.time.Instant;
import java.util.*;

public class SourceRuleTest {
    private static final Generator<Date> DATE_GENERATOR = s -> Date.from(Instant.ofEpochSecond(s.nextInt(0, Integer.MAX_VALUE)));
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
        clearedBefore = source.nextInt();
    }

    @Test
    public void runsOnANewlyInstansiatedClass() {
        Assert.assertEquals(0, aList.size());
        aList.add(source.nextLong());
    }

    @Test
    public void compilesAndRuns() {
        Assert.assertTrue(true);
    }

    @Test
    public void canGetAnInt() {
        int anInt = source.nextInt();
        Assert.assertTrue(anInt >= Integer.MIN_VALUE);
        Assert.assertTrue(anInt <= Integer.MAX_VALUE);
    }

    @Test
    public void canGetIntInRange() {
        int anInt = source.nextInt(1, 10);

        Assert.assertTrue(anInt >= 1);
        Assert.assertTrue(anInt <= 10);
    }

    @Test
    public void canGetAnBetweenMaxAndMin() {
        int anInt = source.nextInt(0, 1);
        Assert.assertTrue(anInt == 0 || anInt == 1);
    }

    @Test(expected = AssertionError.class)
    public void canFail() {
        Assert.assertTrue(false);
    }

    @Test(expected = AssertionError.class)
    public void canFailWithNiceishMessageWhenUsingAGenerator() {
        Assert.assertNull(source.nextBool());
    }

    @Test
    public void canGetABoolean() {
        final boolean bool = source.nextBool();
        Assert.assertTrue(bool || !bool);
    }

    @Test
    public void canGetAByte() {
        final byte aByte = source.nextByte();
        Assert.assertTrue(aByte >= Byte.MIN_VALUE);
        Assert.assertTrue(aByte <= Byte.MAX_VALUE);

        final byte inRange = source.nextByte(5, 12);
        Assert.assertTrue(5 <= inRange);
        Assert.assertTrue(inRange <= 12);
    }

    @Test
    public void canGetAList() {
        final List<Date> list = source.nextList(SourceRuleTest.GENERATE_DATE);
        Assert.assertNotNull(list);

        final List<GeneratorMapTestClass> fixedSize = source.nextList(s -> s.generate(GeneratorMapTestClass.class), 5);
        Assert.assertTrue(fixedSize.size() == 5);

        final List<Byte> aListAOfBytes = source.nextList(ByteSource::nextByte, 1, 10);
        Assert.assertNotNull(aListAOfBytes);
        Assert.assertTrue(1 <= aListAOfBytes.size());
        Assert.assertTrue(aListAOfBytes.size() <= 10);
    }

    @Test
    public void canGetAnArray() {
        final Date[] anArray = source.nextArray(Date.class, SourceRuleTest.GENERATE_DATE);
        Assert.assertNotNull(anArray);

        final GeneratorMapTestClass[] fixedSize = source.nextArray(
                GeneratorMapTestClass.class, s -> s.generate(GeneratorMapTestClass.class), 5);
        Assert.assertTrue(fixedSize.length == 5);

        ToByteFunction<Source> nextByte = Source::nextByte;
        final byte[] aByteArray = source.nextByteArray(nextByte, 1, 10);
        Assert.assertNotNull(aByteArray);

        final byte[] byteArrayWithGenerator = source.nextByteArray(Source::nextByte);
        Assert.assertNotNull(byteArrayWithGenerator);

        final int[] anIntArray = source.nextIntArray(Source::nextInt);
        Assert.assertNotNull(anIntArray);
        Assert.assertTrue(1 <= aByteArray.length);
        Assert.assertTrue(aByteArray.length <= 10);
    }

    @Test
    public void canGenerateWithFunction() {
        final Date generated = source.generate(SourceRuleTest.GENERATE_DATE);
        Assert.assertNotNull(generated);

        final Date functionGenerated = source.generate(DATE_GENERATOR);
        Assert.assertNotNull(functionGenerated);

        final String composedFunctionGenerated = source.generate(DATE_STRING_GENERATOR);
        Assert.assertNotNull(composedFunctionGenerated);
    }

    @Test
    public void canGetAShort() {
        final short aShort = source.nextShort();
        Assert.assertTrue(aShort >= Short.MIN_VALUE);
        Assert.assertTrue(aShort <= Short.MAX_VALUE);

        final short shortInRange = source.nextShort(7, 11);
        Assert.assertTrue(7 <= shortInRange);
        Assert.assertTrue(shortInRange <= 11);
    }

    @Test
    public void canGetAFloat() {
        final float aFloat = source.nextFloat();
        if (Double.isFinite(aFloat) && !Double.isNaN(aFloat)) {
            Assert.assertTrue(aFloat >= -Float.MAX_VALUE);
            Assert.assertTrue(aFloat <= Float.MAX_VALUE);
        }
    }

    @Test
    public void canGetADouble() {
        final double aDouble = source.nextDouble();
        if (Double.isFinite(aDouble) && !Double.isNaN(aDouble)) {
            Assert.assertTrue(aDouble >= -Double.MAX_VALUE);
            Assert.assertTrue(aDouble <= Double.MAX_VALUE);
        }
    }

    @Test
    public void canGetRealDouble() {
        final double realDouble = source.nextRealDouble();

        Assert.assertFalse(Double.isInfinite(realDouble));
        Assert.assertFalse(Double.isNaN(realDouble));

        Assert.assertTrue(realDouble >= -Double.MAX_VALUE);
        Assert.assertTrue(realDouble <= Double.MAX_VALUE);
    }

    @Test
    public void canGetALong() {
        final long aLong = source.nextLong();
        Assert.assertTrue(aLong >= Long.MIN_VALUE);
        Assert.assertTrue(aLong <= Long.MAX_VALUE);

        final long longFromClass = source.reflectively(Long.class);

        Assert.assertTrue(longFromClass >= Long.MIN_VALUE);
        Assert.assertTrue(longFromClass <= Long.MAX_VALUE);
    }

    @Test
    public void canGetLongArray() {
        final long[] someLongs = source.nextLongArray();
        for (long aLong : someLongs) {
            Assert.assertTrue(aLong >= Long.MIN_VALUE);
            Assert.assertTrue(aLong <= Long.MAX_VALUE);
        }
    }

    @Test
    public void canGetEveryKindOfChar() {
        final char c = source.nextChar();
        final char ascii = source.nextChar(CodePoints.ASCII);

        final char alpha = source.nextChar(CodePoints.ALPHA);
        Assert.assertTrue(Character.isAlphabetic(alpha));

        final char alphaNum = source.nextChar(CodePoints.ALPHANUMERIC);
        Assert.assertTrue(Character.isAlphabetic(alphaNum) || Character.isDigit(alphaNum));

        final char customRange = source.nextChar(s -> s.nextShort(48, 57));
        Assert.assertTrue(Character.isDigit(customRange));
    }

    @Test
    public void canGetEveryKindOfString() {
        final String s = source.nextString();
        Assert.assertTrue(s != null);
        final String ascii = source.nextString(CodePoints.ASCII);
        Assert.assertTrue(ascii != null);

        final String alpha = source.nextString(CodePoints.ALPHA);
        for (char c : alpha.toCharArray()) {
            Assert.assertTrue(Character.isAlphabetic(c));
        }

        final String alphaNum = source.nextString(CodePoints.ALPHANUMERIC);
        for (char c : alphaNum.toCharArray()) {
            Assert.assertTrue(Character.isAlphabetic(c) || Character.isDigit(c));
        }

        final String fixedSize = source.nextString(CodePoints.ASCII, 5);
        Assert.assertTrue(fixedSize.length() == 5);
    }

    private enum AnEnum {A, B, C}

    @Test
    public void canGetAnEnum() {
        AnEnum anEnum = source.nextEnum(AnEnum.class);

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
        final Map<String, String> m = source.nextMap(
                s -> s.nextString(CodePoints.ALPHANUMERIC),
                s -> s.nextString(CodePoints.ALPHA));

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

        final Map<Integer, Character> fixedSize = source.nextMap(Source::nextInt, (Generator<Character>) CharSource::nextChar, 5);
        Assert.assertTrue(fixedSize.size() == 5);


        final Map<Integer, Character> sized = source.nextMap(Source::nextInt, (Generator<Character>) Source::nextChar, 5, 10);
        Assert.assertTrue(5 <= sized.size());
        Assert.assertTrue(sized.size() <= 10);
    }

    @Test
    public void canUseKeysToSelectGeneratorsForAMap() {
        final Map<String, Generator<Character>> keysToGenerators = new HashMap<>();
        keysToGenerators.put("ALPHA", s -> s.nextChar(CodePoints.ALPHA));
        keysToGenerators.put("DIGITS", s -> s.nextChar(CodePoints.DIGITS));

        Map<String, Character> map = source.nextMap(s -> s.from(keysToGenerators.keySet()),
                (s, k) -> "ALPHA".equals(k) ? s.nextChar(CodePoints.ALPHA) : s.nextChar(CodePoints.DIGITS));

        Assert.assertTrue(map.get("ALPHA") == null || Character.isAlphabetic(map.get("ALPHA")));
        Assert.assertTrue(map.get("DIGITS") == null || Character.isDigit(map.get("DIGITS")));
    }

    @Test
    public void canGetAMapWhereTheValueGenTakesKeyAsAnArg() {
        final Map<String, String> m = source.nextMap(
                s -> s.nextString(CodePoints.ALPHANUMERIC),
                (s, k) -> k + s.nextString(CodePoints.ALPHA));

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
        Assert.assertEquals(-1921583219793701470L, source.nextLong());
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

        source.reflectively(ClassWithStaticConstructor.class.getMethod("doAThing"));
        source.reflectively(ClassWithStaticConstructor.class.getMethod("doAThing"), aClass);
        Assert.assertEquals(1, aClass.didAThing);
    }

    @Test
    public void canGetSet() {
        final Set<Integer> set = source.nextSet(IntSource::nextInt);
        Assert.assertNotNull(set);

        set.forEach(anInt -> {
            Assert.assertTrue(anInt >= Integer.MIN_VALUE);
            Assert.assertTrue(anInt <= Integer.MAX_VALUE);
        });

        final Set<String> fixedSize = source.nextSet(Source::nextString, 5);
        Assert.assertTrue(fixedSize.size() == 5);

        final Set<String> sized = source.nextSet(Source::nextString, 5, 10);
        Assert.assertTrue(5 <= sized.size());
        Assert.assertTrue(sized.size() <= 10);
    }

    @Test
    public void canGetNullable() {
        final String nullableString = source.nullable(StringSource::nextString);
        Assert.assertTrue(nullableString == null || nullableString != null);
    }

    @Test
    public void canGet2DArray() throws Exception {
        final int[][] array = source.nextArray(int[].class, IntArraySource::nextIntArray);
        Assert.assertNotNull(array);

        final int[][] anotherArray = source.nextArray(int[].class, s -> s.nextIntArray(Source::nextInt));
        Assert.assertNotNull(anotherArray);
    }

    @Test
    public void canGenerateWithNew() {
        final NewClass n = source.generate(NewClass::new);
        Assert.assertTrue(n != null);
        Assert.assertTrue(0 <= n.anInt);
    }

    @Test
    public void canReflectivelyGenerateClassesWithGenerics() {
        final TrickyGenericsWrapper classWithTrickyGenerics = source.reflectively(TrickyGenericsWrapper.class);

        Assert.assertNotNull(classWithTrickyGenerics);
        Assert.assertNotNull(classWithTrickyGenerics.classWithTrickyGenerics.i);
        Assert.assertNotNull(classWithTrickyGenerics.classWithTrickyGenerics.listNewClass);
        Assert.assertNotNull(classWithTrickyGenerics.classWithTrickyGenerics.optionalInt);
        Assert.assertNotNull(classWithTrickyGenerics.classWithTrickyGenerics.optionalString);

        final WithGeneric classWithGenerics = source.reflectively(WithGeneric.class);

        Assert.assertNotNull(classWithGenerics);
    }

    @Test
    public void canReflectivelyGenerateAClassOnlyStaticConstructors() {
        ClassWithStaticConstructor staticy = source.reflectively(ClassWithStaticConstructor.class);

        Assert.assertNotNull(staticy);
    }

    public static class WithGeneric
    {
        public WithGeneric(Map<String, Integer> aMap)
        {

        }
    }

    public static class TrickyGenericsWrapper
    {
        final ClassWithTrickyGenerics<String, Integer, GeneratorMapTestClass> classWithTrickyGenerics;


        public TrickyGenericsWrapper(ClassWithTrickyGenerics<String, Integer, GeneratorMapTestClass> classWithTrickyGenerics) {
            this.classWithTrickyGenerics = classWithTrickyGenerics;
        }
    }

    public static class ClassWithTrickyGenerics<T, Z, V>
    {
        Integer i;
        Optional<T> optionalString;
        Optional<Z> optionalInt;
        List<V> listNewClass;

        public ClassWithTrickyGenerics(Integer anInt, Optional<T> optionalString) {
            this.i = anInt;
            this.optionalString = optionalString;
            this.optionalInt = Optional.empty();
            this.listNewClass = Collections.emptyList();
        }

        public ClassWithTrickyGenerics(Optional<Z> optionalInt, List<V> listNewClass) {
            this.i = 0;
            this.optionalString = Optional.empty();
            this.optionalInt = optionalInt;
            this.listNewClass = listNewClass;
        }
    }

    public class NewClass
    {
        final int anInt;
        public NewClass(Source s)
        {
            anInt = source.nextInt(0, Integer.MAX_VALUE);
        }
    }

    public static final Generator<Date> GENERATE_DATE = s -> Date.from(Instant.ofEpochMilli(s.nextLong()));

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
        public int didAThing = 0;
        public static ClassWithStaticConstructor constructor(ClassWithConstructor c) {
            return new ClassWithStaticConstructor();
        }

        public void doAThing()
        {
            didAThing += 1;
        }
    }
}
