package edu.ntnu.app.lpta;

import java.util.TreeSet;
import java.util.stream.Collectors;

public class LptaPattern {

    private final double period;
    private final int themeIndex;
    private final TreeSet<LocationMean> locationTrajectory;
    private double offset;
    private double[] timeDist;

    public LptaPattern(double period, int themeIndex) {
        this.period = period;
        this.themeIndex = themeIndex;
        this.locationTrajectory = new TreeSet<>();
    }

    public void addLocation(int l, double mean, double stdDev) {
        locationTrajectory.add(new LocationMean(l, mean, stdDev));
    }

    public double getPeriod() {
        return period;
    }

    public double getOffset() {
        return offset;
    }

    public int getThemeIndex() {
        return themeIndex;
    }

    public int[] getLocationTrajectory() {
        return locationTrajectory.stream().mapToInt(lm -> lm.getLocation()).toArray();
    }

    public void setOffset() {
        this.offset = locationTrajectory.first().getMean();
    }

    public void setTimeDist(double[] timeDist) {
        this.timeDist = timeDist;
    }

    @Override
    public String toString() {
        return "LptaPattern{" +
                "period=" + period +
                ", offset=" + offset +
                ", theme=" + themeIndex + Topics.getTopTermsInTopicAsString(themeIndex) +
                ", locationTrajectory=" + locationTrajectory.stream().map(l -> l.toString()).collect(Collectors.joining(", ", "[", "]")) +
                '}';
    }

    class LocationMean implements Comparable<LocationMean> {
        private final int location;
        private final double mean;
        private final double stdDev;

        public LocationMean(int location, double mean, double stdDev) {
            this.location = location;
            this.mean = mean;
            this.stdDev = stdDev;
        }

        public int getLocation() {
            return location;
        }

        public double getMean() {
            return mean;
        }

        @Override
        public int compareTo(LocationMean locationMean) {
            int compare = Double.compare(this.mean, locationMean.mean);
            return compare != 0 ? compare : Integer.compare(this.location, locationMean.location);
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f): %s", mean, stdDev, LptaDocs.getLocation(location).toString());
        }
    }
}
