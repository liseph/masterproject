package edu.ntnu.app.autoperiod;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FindPeriodsInTimeseries {

    static public Float[] execute(Timeseries ts) {
        return execute(ts, 0.1);
    }

    static public Float[] execute(Timeseries ts, double periodThreshold) {
        ts.removeMean();
        Periodogram periodogram = Periodogram.calculatePeriodogram(ts);
        double spectrumThreshold = generateThreshold(ts);
        double[] possiblePeriods = periodogram.extractPossiblePeriods(spectrumThreshold, periodThreshold);

        Autocorrelation autocorrelation = Autocorrelation.calculateAutocorrelation(ts);
        Float[] periods = Arrays.stream(autocorrelation.findPeriodsOnHill(possiblePeriods, periodThreshold)).mapToObj(p -> p).toArray(Float[]::new);

        return periods;
    }

    // Generate threshold as the 99th percentile highest Pxx value for a permuted (non-periodic) sequence.
    static private double generateThreshold(Timeseries ts) {
        double[] cp = ts.getData().clone();
        double threshold = 0;
        double highestValue = 0;
        for (int i = 0; i < 100; i++) {
            shuffleArray(cp);
            Periodogram periodogram = Periodogram.calculatePeriodogram(cp, ts.getFs());
            double maxPxxValue = Arrays.stream(periodogram.getPxx()).max().getAsDouble();
            if (maxPxxValue > threshold && maxPxxValue < highestValue) {
                threshold = maxPxxValue;
            } else if (maxPxxValue > highestValue) {
                threshold = highestValue;
                highestValue = maxPxxValue;
            }
        }
        return threshold;
    }

    // https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
    static private void shuffleArray(double[] ar) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            double a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}
