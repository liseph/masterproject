package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.TreeSet;

public class PstaPattern {
    private final double period;
    private final long offset;
    private final int themeIndex;
    private final TreeSet<Integer> locationTrajectory;

    public PstaPattern(double period, long offset, int themeIndex) {
        this.period = period;
        this.offset = offset;
        this.themeIndex = themeIndex;
        this.locationTrajectory = new TreeSet<>();
    }

    public void addLocation(int l) {
        locationTrajectory.add(l);
    }

    public double getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "PstaPattern{" +
                "period=" + period +
                ", offset=" + offset +
                ", themeIndex=" + themeIndex +
                ", locationTrajectory=" + Arrays.toString(locationTrajectory.toArray()) +
                '}';
    }
}
