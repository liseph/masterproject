package edu.ntnu.app.autoperiod;

import java.util.Arrays;

public class Timeseries {
    final private double[] data;
    final private int fs;
    private boolean meanRemoved = false;

    public Timeseries(double[] data, int fs) {
        this.data = data;
        this.fs = fs;
    }

    public boolean removeMean() {
        if (!meanRemoved) {
            double avg = Arrays.stream(data).average().orElse(Double.NaN);
            Arrays.setAll(data, ix -> data[ix] - avg);
            meanRemoved = true;
            return true;
        }
        return false;
    }

    public double[] getData() {
        return data;
    }

    public int getFs() {
        return fs;
    }
}
