# undertaker-junit

Property testing for Java, made simple:

```java
class ATest {
    @Rule
    Source source = new SourceRule();
    
    @Test
    public void testIntsAreEven()
    {
        Assert.assertTrue(source.nextInt() % 2 == 0);
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
To rerun this particular failing case you can add an annotation to the test:
@Test
@net.lfn3.undertaker.undertaker.junit.Seed(8006649660925287978)
public void canFailWithNiceishMessageWhenUsingAGenerator() { ... }
```

The messages produced by Undertaker are fairly verbose, and you might see some unusual stuff in the simplest and initial
failing values parts. These come from [Clojure](https://clojure.org/), so if you already know how to read it, you should 
be fine. Otherwise: `#{1 2 3}` is a set (of integers). `{"hello" 1}` is a map, containing `"hello"` as a key pointing to 
the value `1`.

## The Source Rule

The source is used to supply all the input to your tests, and also provides the repeated run functionality.
It can also be used to hang onto your commonly used Generators by supplying a `Map<Class, Generator>`:

```java
private static final Map<Class, Generator> GENERATORS;

static {
    GENERATORS = new HashMap<>();
    
    GENERATORS.put(YourClass.class, s -> new YourClass(...));
}

@Rule
public Source source = new SourceRule(GENERATORS);
```

These generators can then be invoked by calling `source.generate(YourClass.class)`. What's a generator? Glad you asked.

## Generators
A generator is simply a function from a Source to anything else. Since it'd be really annoying to use otherwise, the source
has a lot of primitive generators and some 'higher order' collection generators. All of the generators in undertaker 
follow a similar pattern. Numeric generators have three arities. The no argument version produces all the values allowed 
by that type:

```java
source.nextInt();
```
The single argument, max value case produces any value up to and including the value specified:

```java
source.nextInt(maxValue);
```

This always includes negative values, so if you only want positive values you'll need the next arity.
In the two argument case the method will produce values between min and max, which are again inclusive not exclusive.

```java
source.nextInt(minValue, maxValue);
```

The collection generators all take a generator as their first argument. The primitive array generators are the only 
exception to this rule: `source.getDoubleArray()` is fine.

```java
source.nextList(IntSource::nextInt);
```

The second argument is for producing a collection of fixed size. 

```java
source.nextList(LongSource::nextLong, size);
```

When you add a third argument, the second arg is treated as the minimum allowed size of the collection, and the third 
argument as the maximum allowed size of the collection.

```java
source.nextList(StringSource::nextString, minSize, maxSize);
```

The map source is a slight exception to these rules, since you have to feed it two generator functions rather than one:

```java
source.nextMap(ShortSource::nextShort, StringSource::nextString);
```

There is also an option to provide a `BiFunction<Source, K>` as the second generator, where `K` is the type of the key.
This means you can make more heterogeneous collections, by varying the value based on the key that is generated:

```java
final Map<String, Generator<Character>> keysToGenerators = new HashMap<>();
keysToGenerators.put("ALPHA", s -> s.nextChar(CodePoints.ALPHA));
keysToGenerators.put("DIGITS", s -> s.nextChar(CodePoints.DIGITS));

Map<String, Character> map = source.nextMap(s -> s.from(keysToGenerators.keySet()),
        (s, k) -> "ALPHA".equals(k) ? s.nextChar(CodePoints.ALPHA) : s.nextChar(CodePoints.DIGITS));
```

This also shows how `nextChar` works - there's some default `CodePoint` generators in `net.lfn3.undertaker.junit.CodePoints`,
however you can just supply your own short generator. The String generator operates the same way:

```java
final String alphanumeric = source.nextString(CodePoints.ALPHANUMERIC);
final String fourDigits = source.nextString(CodePoints.DIGITS, 4);
final String asciiMax24 = source.nextString(CodePoints.ASCII, 0, 24);
```

There's also collection style overloads on strings, as shown. There's a few other utility generators, like `from` which
picks elements from a list:

```java
Integer i = source.from(Arrays.asList(1, 2, 3)); //i will be either 1, 2, or 3.
```

Note you don't need to use this on `Enum` types, you can just use `source.getEnum(AnEnum.class)`.
By default null values aren't generated by any of the generators in Undertaker. You can 'fix' that with:

```java
String possiblyNullString = source.nullable(Source::getString);
```

There's one more magic weapon in undertakers arsenal: reflection.

```java
SomeClassThatsTooBig instance = source.reflectively(SomeClassThatsTooBig.class);
``` 

It does of course have some caveats. It only works on concrete classes. It may run into something it can't figure how to
generate reflectively, in which case I'd recommend adding that class to the Generator map you can feed into your source.
The reflective generators are great for testing marshalling or serialization of objects, without the tedious process of
manually filling them out.

There's examples of all of the generators in this codebase [here](src/test/java/net/lfn3/undertaker/junit/SourceRuleTest.java)
or you can always spin up debugger and sample input from the source to get an idea of how the various generators work.

## Writing your own generators

Generators are simply functions from `Source` to `T`. This basically means you can write them however you want, but 
so far I think I prefer static factory functions:

```java
public static final Generator<Date> GENERATE_DATE = s -> Date.from(Instant.ofEpochMilli(s.nextLong()));

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
        anInt = source.nextInt(0, Integer.MAX_VALUE);
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
object graphs. There's one technique we've used successfully so far, which is intermediate `Scenario` objects:

```java
public class MatchingOrdersScenario
{
    public final Source source;
    public final long targetPrice;
    public final Side targetSide;
    
    public MatchingOrdersScenario(Source source)
    {
        this.source = source;
        targetPrice = source.nextLong(0, 500);
        targetSide = source.generate(Side.class);
    }
    
    public Order generateOrder()
    {
        return new Order(targetPrice, targetSide);
    }
    
    public MassQuoteOrder generateLiquidity()
    {
        return new MassQuoteOrder(
                new Order(targetPrice, targetSide.opposite()), 
                new Order(source.generate(s -> targetPrice + s.nextLong(0, 60), targetSide))); // and so on
    }
}
```

These let you knit together several related objects at while generating them, and then pull out the individual parts as
needed during a test. They also compose relatively well, since you can pass the Source further down to other scenarios.

## Intervals

One thing you might have noticed in the above scenario is the use of `source.generate(...)`. This is used to ensure
the value we've tweaked by adding target price shows up in our output. What determines if the an elements appears in the
printed output? It's got to be encased in an interval (one of Undertaker's data structures) and at the top level
(not inside another interval). You probably don't need to worry about this too much when you're writing a test.
I'd recommend that if you encounter a test failure you go back through and add calls to generate (or the more primitive
`pushInterval` and `popInterval` operations, which appear inside generate) as necessary to give you output that makes 
sense.

By default Undertaker doesn't show the intervals used in a particular test case. You can use the `@Debug(true)` 
annotation on a test to show a *lot* more information about what exactly is going on inside Undertaker during a test,
including the intervals used, or if you're curious and just want to see how it works.

<!--TODO: How to use intervals to shrink collections -->

## Acknowledgements

[David R. MacIver](https://www.drmaciver.com/) for writing [Hypothesis](https://hypothesis.works/) and 
[Hypothesis-Java](https://github.com/HypothesisWorks/hypothesis-java) without which there is approximately zero chance 
I would have realized there was a better way of doing property testing.

[Reid Draper](https://twitter.com/reiddraper), [Gary Fredericks](https://twitter.com/gfredericks_) and all of the
[contributors](https://github.com/clojure/test.check/graphs/contributors) to 
[test.check](https://github.com/clojure/test.check/). test.check was where I started with property testing, and the 
place I "borrowed" big chunks of Undertaker's api from.

My employer, [LMAX](https://www.lmax.com/) for paying for the significant amount time I've spent working on this thing.
In particular [Mike Barker](https://twitter.com/mikeb2701) for doing significant rubber duck duty.

## License

Copyright © 2017 Liam Falconer

Distributed under the Apache License, Version 2.0
