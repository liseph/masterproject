package edu.ntnu.app.lpta;

import edu.ntnu.app.Document;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class TimeDistTopicLocs {

    private static final double NORMALIZE = 1.0 / Math.sqrt(2 * Math.PI);
    public static double STD_DEVIATION_MIN = 0.01;
    public static int[][] flag;
    private static double[][][] timeDistTopicLocs;
    private static double[][] means;
    private static double[][] stdDeviations;
    private static double[] periods;
    private static double backgroundTopic;
    private static boolean converges = false;
    private static int nPeriodicTopics;

    public static void initialize(int nPeriodicTops, double[] ps) {
        flag = new int[LptaDocs.nLocations()][nPeriodicTops];
        nPeriodicTopics = nPeriodicTops;
        periods = ps;
        timeDistTopicLocs = new double[LptaDocs.nLocations()][nPeriodicTopics][LptaDocs.nTimeslots()];
        means = new double[LptaDocs.nLocations()][nPeriodicTopics];
        stdDeviations = new double[LptaDocs.nLocations()][nPeriodicTopics];
        // Background topic is constant uniform, set only once here.
        backgroundTopic = 1.0 / LptaDocs.nTimeslots();
        // Set time distribution to uniform distribution, no need to set means and std deviations, they are updated later.
        double[] zs = new double[LptaDocs.nTimeslots()];
        Arrays.fill(zs, 1.0 / LptaDocs.nTimeslots());
        for (int z = 0; z < nPeriodicTops; z++) {
            for (int l = 0; l < LptaDocs.nLocations(); l++) {
                timeDistTopicLocs[l][z] = zs;
            }
        }
    }

    public static void update() {
        converges = true;
        for (int l = 0; l < LptaDocs.nLocations(); l++) {
            List<Integer> docsInLoc = LptaDocs.getDocsInLoc(l);
            for (int z = 0; z < nPeriodicTopics; z++) {
                updateMeanAndStandardDeviation(docsInLoc, z, l);
                updateDistribution(z, l);
            }
        }
    }

    private static void updateMeanAndStandardDeviation(List<Integer> docsInLoc, int z, int l) {
        double meanN = 0;
        double stdDevN = 0;
        double denominator = 0;
        for (int d : docsInLoc) {
            double base = calcAllWords(d, z);
            meanN += base * (LptaDocs.getDoc(d).getTimestamp() % (int) periods[z]);
            stdDevN += base * sqr(LptaDocs.getDoc(d).getTimestamp() % (int) periods[z] - means[l][z]);
            denominator += base;
        }
        if (denominator == 0) {
            flag[l][z] = 1;
            means[l][z] = 0;
            stdDeviations[l][z] = 0;
        } else {
            flag[l][z] = 0;
            means[l][z] = meanN / denominator;
            stdDeviations[l][z] = Math.sqrt(stdDevN / denominator);
        }
    }

    private static void updateDistribution(int z, int l) {
        double[] newVal = new double[LptaDocs.nTimeslots()];
        for (int t = 0; t < LptaDocs.nTimeslots(); t++) {
            newVal[t] = calcPeriodic(z, l, t);
            converges = converges && Math.abs(newVal[t] - timeDistTopicLocs[l][z][t]) < Lpta.CONVERGES_LIM;
        }
        timeDistTopicLocs[l][z] = newVal;
    }

    private static double calcPeriodic(int z, int l, int t) {
        double p = periods[z];
        double probK = p / LptaDocs.nTimeslots();
        if (stdDeviations[l][z] < STD_DEVIATION_MIN) {// Too few samples to calculate a probability, return uniform distribution.
            flag[l][z] = 1;
            return probK;
        }
        flag[l][z] = 0;
        double tVal = LptaDocs.getTimestamp(t);
        return probK * (NORMALIZE / stdDeviations[l][z]) * Math.exp(-sqr((tVal % p) - means[l][z]) / sqr(stdDeviations[l][z]));
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
        return converges;
    }

    public static double get(int z, int d) {
        if (z == nPeriodicTopics) return backgroundTopic;
        Document doc = LptaDocs.getDoc(d);
        return timeDistTopicLocs[doc.getLocationId()][z][doc.getTimestampId()];
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
                        .mapToDouble(l -> timeDistTopicLocs[l][z][t])
                        .sum() / nLocs)
                .toArray();
    }

    public static void clear() {
        timeDistTopicLocs = null;
        means = null;
        stdDeviations = null;
        periods = null;
        backgroundTopic = 0;
        converges = false;
        nPeriodicTopics = 0;
    }
}
