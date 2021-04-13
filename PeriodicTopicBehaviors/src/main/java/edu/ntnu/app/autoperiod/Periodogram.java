package edu.ntnu.app.autoperiod;

import hageldave.ezfftw.dp.FFT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.ntnu.app.autoperiod.Autocorrelation.getNextRegular;

public class Periodogram {
    final private double[] Pxx;
    final private double[] f;

    private Periodogram(double[] pxx, double[] f) {
        Pxx = pxx;
        this.f = f;
    }

    public static Periodogram calculatePeriodogram(Timeseries ts) {
        ts.removeMean();
        return calculatePeriodogram(ts.getData(), ts.getFs());
    }

    private static double sqr(double x) {
        return x * x;
    }

    public static Periodogram calculatePeriodogram(double[] data, int fs) {
        int nobs = data.length;
        int n = getNextRegular(2 * nobs + 1);

        // Pad data with 0's for the FFT function.
        double[] paddedData = new double[n];
        System.arraycopy(data, 0, paddedData, 0, nobs);
        double[] realPart = new double[n];
        double[] imagPart = new double[n];
        FFT.fft(paddedData, realPart, imagPart, n);

        // Calculate the periodogram as the squared length of each Fourier coefficient
        double[] pxx = new double[n];
        // pxx[0] = 0;
        for (int i = 0; i < n; i++) {
            pxx[i] = (sqr(realPart[i]) + sqr(imagPart[i])) / n;
        }

        // Get frequencies f, inspired by https://github.com/numpy/numpy/blob/main/numpy/fft/helper.py#L123-L169
        double freqScale = 1 / ((double) n * 1 / fs);
        double[] freq = new double[n];
        int N = (n - 1) / 2 + 1;
        for (int i = 0; i < N; i++)
            freq[i] = i * freqScale;
        for (int i = N, j = -n / 2; i < n && j < 0; i++, j++) {
            freq[i] = j * freqScale;
        }
        double[] f = new double[n];
        System.arraycopy(freq, 0, f, 0, n);
        f[n - 1] = Math.abs(f[n - 1]);

        return new Periodogram(pxx, f);
    }

    public static Periodogram calculatePeriodogramWelch(double[] data, int fs) {
        int n = data.length;

        int psdSize = n / 2 + 1;

        double[] window = generateHanningWindow(n);
        double windowScale = sqr(Arrays.stream(window).sum());

        // consider if I should remove mean here
        double[] windowedSegment = new double[n];
        for (int i = 0; i < n; i++) {
            windowedSegment[i] = data[i] * window[i];
        }
        // Apply the FFT
        double[] realOut = new double[n];
        double[] imagOut = new double[n];
        FFT.fft(windowedSegment, realOut, imagOut, n);
        double[] fftSegment = new double[n];
        for (int i = 0; i < n; i++) {
            fftSegment[i] = sqr(realOut[i]) + sqr(imagOut[i]);
        }

        double powerDensityNormalization = 1 / windowScale;
        double powerDensityTransformation = 1 / (double) fs;

        double[] fftWelchEstimateOneSided = new double[psdSize];
        System.arraycopy(fftSegment, 0, fftWelchEstimateOneSided, 0, psdSize);
        double[] pxx = Arrays.stream(fftWelchEstimateOneSided).map(v -> Math.abs(v) * powerDensityNormalization * powerDensityTransformation).toArray();
        // Double frequencies except DC and Nyquist
        for (int i = 2; i < psdSize - 1; i++) {
            pxx[i] *= 2;
        }
        // Get frequencies f, inspired by https://github.com/numpy/numpy/blob/main/numpy/fft/helper.py#L123-L169
        double freqScale = 1 / ((double) n * 1 / fs);
        double[] freq = new double[n];
        int N = (n - 1) / 2 + 1;
        for (int i = 0; i < N; i++)
            freq[i] = i * freqScale;
        for (int i = N, j = -n / 2; i < n && j < 0; i++, j++) {
            freq[i] = j * freqScale;
        }
        double[] f = new double[psdSize];
        System.arraycopy(freq, 0, f, 0, psdSize);
        f[psdSize - 1] = Math.abs(f[psdSize - 1]);

        return new Periodogram(pxx, f);
    }

    private static double[] generateHanningWindow(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (size - 1)));
        }
        return signal;
    }

    public double[] getPxx() {
        return Pxx;
    }

    public double[] getF() {
        return f;
    }


    // Filter out periods based on thresholds
    public double[] extractPossiblePeriods(double spectrumThreshold, double periodThreshold) {
        List<Double> possiblePeriods = new ArrayList<>();
        for (int i = 0; i < Pxx.length; i++) {
            double period = 1 / f[i];
            if (Pxx[i] > spectrumThreshold) {
                if (period > periodThreshold && period < Pxx.length)
                    possiblePeriods.add(period);
            }
        }
        return possiblePeriods.stream().mapToDouble(d -> d).toArray();
    }
}
