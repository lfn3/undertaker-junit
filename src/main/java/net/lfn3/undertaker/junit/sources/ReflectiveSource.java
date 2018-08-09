package net.lfn3.undertaker.junit.sources;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReflectiveSource {
    <T> T reflectively(Class<T> klass);
    <T> T reflectively(Constructor<T> constructor);
    <T> T reflectively(Method function);
    <T, R> T reflectively(Method toInvoke, R instance);
}
