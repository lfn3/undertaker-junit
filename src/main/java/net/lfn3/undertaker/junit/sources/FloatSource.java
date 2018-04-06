package net.lfn3.undertaker.junit.sources;

public interface FloatSource {
    float nextFloat();
    float nextFloat(float max);
    float nextFloat(float min, float max);
}
