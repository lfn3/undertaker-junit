package net.lfn3.undertaker.junit.sources;

public interface EnumSource {
    <T extends Enum> T nextEnum(Class<T> enumClass);
}
