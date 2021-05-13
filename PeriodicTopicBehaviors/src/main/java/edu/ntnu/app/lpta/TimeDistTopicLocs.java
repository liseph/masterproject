package edu.ntnu.app.lpta;

import edu.ntnu.app.Document;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class TimeDistTopicLocs {

    private static final double NORMALIZE = 1.0 / Math.sqrt(2 * Math.PI);
    public static double STD_DEVIATION_MIN = 0.01;
    private static double[][][] timeDistTopicLocs;
    private static double[][] means;
    private static double[][] stdDeviations;
    private static double[] periods;
    private static double backgroundTopic;
    private static boolean hasConverged = false;
    private static int nPeriodicTopics;

    public static void initialize(int nPeriodicTops, double[] ps) {
        nPeriodicTopics = nPeriodicTops;
        periods = ps;
        timeDistTopicLocs = new double[nPeriodicTopics][LptaDocs.nLocations()][LptaDocs.nTimeslots()];
        means = new double[LptaDocs.nLocations()][nPeriodicTopics];
        stdDeviations = new double[LptaDocs.nLocations()][nPeriodicTopics];
        // Background topic is constant uniform, set only once here.
        backgroundTopic = 1.0 / LptaDocs.nTimeslots();
        // Set time distribution to uniform distribution, no need to set means and std deviations, they are updated later.
        // double[] zs = IntStream.range(0, LptaDocs.nTimeslots()).mapToDouble(i -> 1.0 / LptaDocs.nTimeslots()).toArray();
        IntStream.range(0, nPeriodicTopics).forEach(z -> {
            IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
                double[] zss = new Random().doubles(LptaDocs.nTimeslots(), 0, 1).toArray();
                double sum = Arrays.stream(zss).sum();
                double[] zs = Arrays.stream(zss).map(t -> t / sum).toArray();
                timeDistTopicLocs[z][l] = zs;
            });
        });
    }

    public static void update() {
        updateMeans();
        updateStdDeviations();
        updateDist();
    }

    private static void updateDist() {
        hasConverged = true;
        IntStream.range(0, nPeriodicTopics).forEach(z -> {
            IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
                double[] newVal = IntStream.range(0, LptaDocs.nTimeslots()).mapToDouble(t -> calcPeriodic(z, l, t)).toArray();
                if (hasConverged) {
                    hasConverged = IntStream
                            .range(0, LptaDocs.nTimeslots())
                            .allMatch(t -> Math.abs(newVal[t] - timeDistTopicLocs[z][l][t]) < Lpta.CONVERGES_LIM);
                }
                timeDistTopicLocs[z][l] = newVal;
            });
        });
    }

    private static double calcPeriodic(int z, int l, int t) {
        double p = periods[z];
        double probK = p / LptaDocs.nTimeslots(); // TODO: Is choice of K uniform? Yes if the number of docs per timestamp is constant?
        // TODO: Change this to getDocsInLocAndTimeWithTopic?
        if (stdDeviations[l][z] < STD_DEVIATION_MIN)
            return probK * LptaDocs.getDocsInLoc(l).length / LptaDocs.nDocuments(); // Too few samples to calculate a probability.
        double tVal = LptaDocs.getTimestamp(t);
        return probK * (NORMALIZE / stdDeviations[l][z]) * Math.exp(-sqr((tVal % p) - means[l][z]) / sqr(stdDeviations[l][z]));
    }

    private static void updateMeans() {
        IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
            int[] docsInLoc = LptaDocs.getDocsInLoc(l);
            IntStream.range(0, nPeriodicTopics).forEach(z -> {
                double[] base = Arrays.stream(docsInLoc).mapToDouble(d -> calcAllWords(d, z)).toArray();
                double denominator = Arrays.stream(base).sum();
                if (denominator != 0) {
                    double numerator = IntStream
                            .range(0, docsInLoc.length)
                            .mapToDouble(d -> base[d] * (LptaDocs.getDoc(d).getTimestamp() % (int) periods[z]))
                            .sum();
                    means[l][z] = numerator / denominator;
                } else {
                    means[l][z] = 0;
                }
            });
        });
    }

    private static void updateStdDeviations() {
        IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
            int[] docsInLoc = LptaDocs.getDocsInLoc(l);
            IntStream.range(0, nPeriodicTopics).forEach(z -> {
                double[] base = Arrays.stream(docsInLoc).mapToDouble(d -> calcAllWords(d, z)).toArray();
                double denominator = Arrays.stream(base).sum();
                if (denominator != 0) {
                    double numerator = IntStream
                            .range(0, docsInLoc.length)
                            .mapToDouble(d -> base[d] * sqr(LptaDocs.getDoc(d).getTimestamp() % (int) periods[z] - means[l][z]))
                            .sum();
                    stdDeviations[l][z] = Math.sqrt(numerator / denominator);
                } else {
                    stdDeviations[l][z] = 0;
                }
            });
        });
    }

    private static double calcAllWords(int d, int z) {
        return Arrays.stream(LptaDocs.getDoc(d).getTermIndices())
                .mapToDouble(w -> LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z))
                .sum();
    }

    private static double sqr(double v) {
        return v * v;
    }

    public static boolean hasConverged() {
        return hasConverged;
    }

    public static double get(int z, int d) {
        if (z == nPeriodicTopics) return backgroundTopic;
        Document doc = LptaDocs.getDoc(d);
        return timeDistTopicLocs[z][doc.getLocationId()][doc.getTimestampId()];
    }

    public static double getStdDeviation(int l, int z) {
        return stdDeviations[l][z];
    }

    public static double getMean(int l, int z) {
        return means[l][z];
    }

    // Returns the avg of time distributions for topic z and specified locations
    public static double[] getTimeDist(int z, int[] locationTrajectory) {
        int nLocs = locationTrajectory.length;
        return IntStream
                .range(0, LptaDocs.nTimeslots())
                .mapToDouble(t -> Arrays
                        .stream(locationTrajectory)
                        .mapToDouble(l -> timeDistTopicLocs[z][l][t])
                        .sum() / nLocs)
                .toArray();
    }

    public static void clear() {
        timeDistTopicLocs = null;
        means = null;
        stdDeviations = null;
        periods = null;
        backgroundTopic = 0;
        hasConverged = false;
        nPeriodicTopics = 0;
    }
}
