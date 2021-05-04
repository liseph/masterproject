package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.TreeSet;

public class PstaPattern {
    private final double period;
    private final int themeIndex;
    private final TreeSet<LocationPattern> locationTrajectory;

    public PstaPattern(double period, int themeIndex) {
        this.period = period;
        this.themeIndex = themeIndex;
        this.locationTrajectory = new TreeSet<>();
    }

    public void addLocation(int l, long offset) {
        locationTrajectory.add(new LocationPattern(l, offset));
    }

    public double getPeriod() {
        return period;
    }

    @Override
    public String toString() {
        return "PstaPattern{" +
                "period=" + period +
                ", themeIndex=" + themeIndex +
                ", locationTrajectory=" + Arrays.toString(locationTrajectory.toArray()) +
                '}';
    }

    class LocationPattern implements Comparable<LocationPattern> {
        private final int locationId;
        private final long offset;

        public LocationPattern(int locationId, long offset) {
            this.locationId = locationId;
            this.offset = offset;
        }

        @Override
        public int compareTo(LocationPattern locationPattern) {
            return Long.compare(this.offset, locationPattern.offset);
        }

        @Override
        public String toString() {
            return "(loc:" + PstaDocs.getLocation(locationId) +
                    ", offset=" + offset +
                    ')';
        }
    }
}
