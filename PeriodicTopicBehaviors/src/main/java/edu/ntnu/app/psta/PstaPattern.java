package edu.ntnu.app.psta;

import java.util.TreeSet;

public class PstaPattern {
    private final Float period;
    private final long offset;
    private final int themeIndex;
    private final TreeSet<Integer> locationTrajectory;

    public PstaPattern(Float period, long offset, int themeIndex) {
        this.period = period;
        this.offset = offset;
        this.themeIndex = themeIndex;
        this.locationTrajectory = new TreeSet<>();
    }

    public void addLocation(int l) {
        locationTrajectory.add(l);
    }

    public Float getPeriod() {
        return period;
    }

    public long getOffset() {
        return offset;
    }

    public int getThemeIndex() {
        return themeIndex;
    }

    public TreeSet<Integer> getLocationTrajectory() {
        return locationTrajectory;
    }
}
