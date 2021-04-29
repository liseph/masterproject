package edu.ntnu.app.lpta;

import edu.ntnu.app.Location;

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

    public void addLocation(int l, double mean) {
        locationTrajectory.add(new LocationMean(l, mean));
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

        public LocationMean(int location, double mean) {
            this.location = location;
            this.mean = mean;
        }

        public int getLocation() {
            return location;
        }

        public double getMean() {
            return mean;
        }

        @Override
        public int compareTo(LocationMean locationMean) {
            return Double.compare(this.mean, locationMean.mean);
        }

        @Override
        public String toString() {
            Location l = LptaDocs.getLocation(location);
            return "" + location +
                    "(" + l.getLongitude() +
                    "," + l.getLatitude() +
                    ")";
        }
    }
}
