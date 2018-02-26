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

There's a lot more apis which you can find details about [here](docs/cheatsheet.md).

## License

Copyright © 2017 Liam Falconer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
