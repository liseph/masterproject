package edu.ntnu.app.autoperiod;

import hageldave.ezfftw.dp.FFT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Autocorrelation {

    final private double[] autocorrelation;
    final private int fs;

    public Autocorrelation(double[] autocorrelation, int fs) {
        this.autocorrelation = autocorrelation;
        this.fs = fs;
    }

    private static double sqr(double x) {
        return x * x;
    }

    // Inspired by python implementation of acf by statsmodels
    // https://github.com/statsmodels/statsmodels/blob/d42dc3f3c63edf0e2eb08f9297705ed9333c4357/statsmodels/tsa/stattools.py#L383
    public static Autocorrelation calculateAutocorrelation(Timeseries ts) {
        double[] data = ts.getData();
        int nobs = data.length;
        int n = getNextRegular(2 * nobs + 1);

        // Pad data with 0's for the FFT function.
        double[] paddedData = new double[n];
        System.arraycopy(data, 0, paddedData, 0, nobs);
        double[] realPart = new double[n];
        double[] imagPart = new double[n];
        FFT.fft(paddedData, realPart, imagPart, n);

        // Calculate the product of the FFT with the conjugated FFT
        double[] tmp = new double[n];
        tmp[0] = 0;
        for (int i = 1; i < n; i++) {
            tmp[i] = sqr(realPart[i]) + sqr(imagPart[i]);
        }

        // Take inverse FFT of the product in tmp
        double[] ac = new double[n];
        FFT.ifft(tmp, new double[n], ac, n);
        // Normalize by variance (ac[0])
        for (int i = 1; i < nobs; i++)
            ac[i] /= ac[0];
        ac[0] = 1;
        // Since we padded with 0's in the beginning, we need to cut this when we return.
        // We return an array the same length as the input data.
        return new Autocorrelation(Arrays.copyOfRange(ac, 0, nobs), ts.getFs());
    }

    // From implementation of acf by statsmodels:
    // https://github.com/statsmodels/statsmodels/blob/d42dc3f3c63edf0e2eb08f9297705ed9333c4357/statsmodels/tsa/stattools.py#L383
    // Find the next regular number greater than or equal to target.
    // Regular numbers are composites of the prime factors 2, 3, and 5.
    // Also known as 5-smooth numbers or Hamming numbers, these are the optimal size for inputs to FFT.
    // Target must be a positive integer.
    static int getNextRegular(int target) {
        if (target <= 6)
            return target;

        // Quickly check if it's already a power of 2
        if ((target & (target - 1)) == 0)
            return target;

        double match = Double.POSITIVE_INFINITY;
        int p5 = 1;
        int p35, quotient, p2, N;
        while (p5 < target) {
            p35 = p5;
            while (p35 < target) {
                quotient = (target + p35 - 1) / p35;
                p2 = (int) Math.pow(2, BigInteger.valueOf(quotient - 1).bitLength());
                N = p2 * p35;
                if (N == target)
                    return N;
                else if (N < match)
                    match = N;
                p35 *= 3;
                if (p35 == target)
                    return p35;
            }
            if (p35 < match)
                match = p35;
            p5 *= 5;
            if (p5 == target)
                return p5;
        }
        if (p5 < match)
            match = p5;
        return (int) match;
    }

    /*
     * Function to find closest hill to each possible period that lies on a hill (negative acceleration).
     * Note that this is a different method than the one described in the paper.
     */
    public double[] findPeriodsOnHill(double[] possiblePeriods, double periodThreshold) {
        List<Double> periods = new ArrayList<>();
        for (double period : possiblePeriods) {
            int i = (int) (period * fs);
            double acceleration = 1;
            double tmp;
            for (int j = -2; j <= 2; j++) {
                if (autocorrelation.length < i + j + 2)
                    break;
                tmp = autocorrelation[i + j + 2] - 2 * autocorrelation[i + j + 1] + autocorrelation[i + j];
                acceleration = Math.min(acceleration, tmp);
            }
            if (acceleration <= 0) {
                while (i + 1 < autocorrelation.length && autocorrelation[i] < autocorrelation[i + 1]) i++;
                while (i > 0 && autocorrelation[i] < autocorrelation[i - 1]) i--;
                double p = ((double) i) / fs;
                if (p != 0 && p > periodThreshold && periods.stream().noneMatch(ps -> ps == p)) {
                    periods.add(p);
                }
            }
        }
        return periods.stream().mapToDouble(d -> d).toArray();
    }
}
