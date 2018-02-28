# undertaker-junit

Property testing for Java, made simple:

```java
class ATest {
    @Rule
    Source source = new SourceRule();
    
    @Test
    public void testIntsAreEven()
    {
        Assert.assertTrue(source.getInt() % 2 == 0);
    }
}
```

This test will get run a bunch of times (1000 by default), if you're lucky. Since you're probably not that lucky, 
it'll fail, and you'll get a message telling you about what values made it fail, the smallest value we could make the 
test fail with, and how to reproduce it:

```
testIntsAreEven failed after running 1 times.

The cause of the failure was:
java.lang.AssertionError

The simplest values we could make the test fail with were:
[1]

The initial failing values were:
[760405255]

The seed that generated the initial case was 8006585014295842258.
If you want to rerun this particular failing case, you can add this seed to the test.

If you're using Clojure, you can add :seed to this test's options map:
(defprop testIntsAreEven {:seed 8006585014295842258} ...)

If you're using Java and jUnit, you can add an annotation to the test:
@Test
@net.lfn3.undertaker.undertaker.junit.Seed(8006585014295842258)
public void testIntsAreEven() { ... }
```

The messages produced by Undertaker are fairly verbose.

## Generators

All of the generators in undertaker follow a similar pattern. Numeric generators have three arities.
The no argument version produces all the values allowed by that type:

```java
source.getInt();
```
The single argument, max value case produces any value up to and including the value specified:

```java
source.getInt(maxValue);
```

This always includes negative values, so if you only want positive values you'll need the next arity.
In the two argument case the method will produce values between min and max, which are again inclusive not exclusive.

```java
source.getInt(minValue, maxValue);
```

The collection generators all take a generator as their first argument. The primitive array generators are the only 
exception to this rule: `source.getDoubleArray()` is fine.

```java
source.getList(IntSource::getInt);
```

The second argument is for producing a collection of fixed size. 

```java
source.getList(LongSource::getLong, size);
```

When you add a third argument, the second arg is treated as the minimum allowed size of the collection, and the third 
argument as the maximum allowed size of the collection.

```java
source.getList(StringSource::getString, minSize, maxSize);
```

The map source is a slight exception to these rules, since you have to feed it two generator functions rather than one:

```java
source.getMap(ShortSource::getShort, StringSource::getString);
```

There is also an option to provide a `BiFunction<Source, K>` as the second generator, where `K` is the type of the key.
This means you can make more heterogeneous collections, by varying the value based on the key that is generated:

```java
final Map<String, Generator<Character>> keysToGenerators = new HashMap<>();
keysToGenerators.put("ALPHA", s -> s.getChar(CodePoints.ALPHA));
keysToGenerators.put("DIGITS", s -> s.getChar(CodePoints.DIGITS));

Map<String, Character> map = source.getMap(s -> s.from(keysToGenerators.keySet()),
        (s, k) -> "ALPHA".equals(k) ? s.getChar(CodePoints.ALPHA) : s.getChar(CodePoints.DIGITS));
```

This also shows how `getChar` works - there's some default `CodePoint` generators in `net.lfn3.undertaker.junit.CodePoints`,
however you can just supply your own short generator. The String generator operates the same way:

```java
final String alphanumeric = source.getString(CodePoints.ALPHANUMERIC);
final String fourDigits = source.getString(CodePoints.DIGITS, 4);
final String asciiMax24 = source.getString(CodePoints.ASCII, 0, 24);
```

There's also collection style overloads on strings, as shown.

There's examples of all of the generators in this codebase [here](src/test/java/net/lfn3/undertaker/junit/SourceRuleTest.java)
or you can always spin up debugger and sample input from the source to get an idea of how the various generators work.

## Writing your own generators

Generators are simply functions from `Source` to `T`. This basically means you can write them however you want, but 
so far I think I prefer static factory functions:

```java
public static final Generator<Date> GENERATE_DATE = s -> Date.from(Instant.ofEpochMilli(s.getLong()));

@Test
public void canGenerateWithFunction() {
    final Date generated = source.generate(SourceRuleTest.GENERATE_DATE);
    Assert.assertNotNull(generated);
}
```

If you're building larger objects you might prefer to provide a constructor:

```java
public class NewClass
{
    final int anInt;
    public NewClass(Source s)
    {
        anInt = source.getInt(0, Integer.MAX_VALUE);
    }
}

@Test
public void canGenerateWithNew() {
    final NewClass n = source.generate(NewClass::new);
    Assert.assertTrue(n != null);
    Assert.assertTrue(0 <= n.anInt);
}
```

However this might mean leaking this library into your "business" objects.

Writing generators will obviously get harder as your generators get bigger or are required to generate more complex 
objects. There's one technique we've use successfully so far, which is the use of intermediate `Scenario` objects.


<!--TODO: Talk about Intervals, custom generators -->

## License

Copyright © 2017 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
