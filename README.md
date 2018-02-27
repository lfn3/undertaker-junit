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

### Generators

All of the generators in undertaker follow a similar pattern. Numeric generators have three arities:
```java
source.getInt();
```
The no argument version produces all the values allowed by that type. 

```java
source.getInt(maxValue);
```

The single argument, max value case produces any value up to and including the value specified. 
This always includes negative values, so if you only want positive values you'll need the next arity. 

```java
source.getInt(minValue, maxValue);
```

In the two argument case the method will produce values between min and max, which are again inclusive not exclusive.

```java
source.getList(IntSource::getInt);
```

The collection generators all take a generator as their first argument. The primitive array generators are the only 
exception to this rule: `source.getDoubleArray()` is fine.

```java
source.getList(LongSource::getLong, size);
```

The second argument is for producing a collection of fixed size. 

```java
source.getList(StringSource::getString, minSize, maxSize)
```

When you add a third argument, the second arg is treated as the minimum allowed size of the collection, and the third 
argument as the maximum allowed size of the collection.

The map source is a slight exception to these rules, since you have to feed it two generator functions rather than one:

```java
source.getMap(ShortSource::getShort, StringSource::getString)
```

There is also an option to provide a `BiFunction<Source, K>` as the second generator, where `K` is the type of the key.
This means you can make more heterogeneous collections.

<!--TODO: CharSource, StringSource -->

There's a cheat sheet with more examples [here](docs/cheatsheet.md), or you can always spin up debugger and sample 
input from the source to get an idea of how the various generators work.

## License

Copyright © 2017 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
