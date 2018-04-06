package net.lfn3.undertaker.junit.sources;

public interface DoubleSource {
    double nextDouble();
    double nextDouble(double max);
    double nextDouble(double min, double max);

    double nextRealDouble();
    double nextRealDouble(double max);
    double nextRealDouble(double min, double max);
}
