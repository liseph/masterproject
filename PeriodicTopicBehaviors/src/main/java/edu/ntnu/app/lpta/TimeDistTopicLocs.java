package edu.ntnu.app.lpta;

import edu.ntnu.app.Document;

import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

public class TimeDistTopicLocs {

    private static double NORM = 1.0 / Math.sqrt(2*Math.PI);
    private static double[][][] timeDistTopicLocs;
    private static double[][] means;
    private static double[][] stdDeviations;
    private static double[] periods;
    private static int nTopics;
    private static double backgroundTopic;
    private static boolean hasConverged = false;
    private static int nPeriodicTopics;
    private static int nBurstyTopics;

    public static void initialize(int nPeriodicTops, int nBurstyTops, double[] ps) {
        if (!LatentWordByTopics.isInitialized()) {
            throw new IllegalStateException("Cannot initialize TimeDistTopicLocs before LatentWordByTopics.");
        }
        nPeriodicTopics = nPeriodicTops;
        nBurstyTopics = nBurstyTops;
        nTopics = nPeriodicTopics + nBurstyTopics;
        periods = ps;
        timeDistTopicLocs = new double[nTopics][LptaDocs.nLocations()][LptaDocs.nTimeslots()];
        means = new double[LptaDocs.nLocations()][nTopics];
        stdDeviations = new double[LptaDocs.nLocations()][nTopics];
        // Background topic is constant uniform, set only once here.
        backgroundTopic = 1.0 / LptaDocs.nTimeslots();
        update();
    }

    public static void update() {
        updateMeans();
        updateStdDeviations();
        updateDist();
    }

    private static void updateDist() {
        hasConverged = true;
        IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
            // Periodic topics:
            IntStream.range(0, nPeriodicTopics).forEach(z -> {
                double[] newVal = IntStream.range(0, LptaDocs.nTimeslots()).mapToDouble(t -> calcPeriodic(z, l, t)).toArray();
                hasConverged = hasConverged && IntStream
                        .range(0, LptaDocs.nTimeslots())
                        .allMatch(t -> Math.abs(newVal[t] - timeDistTopicLocs[z][l][t]) < Lpta.EPSILON);
                timeDistTopicLocs[z][l] = newVal;
            });
            // Bursty topics:
            IntStream.range(nPeriodicTopics, nPeriodicTopics+nBurstyTopics).forEach(z -> {
                double[] newVal = IntStream.range(0, LptaDocs.nTimeslots()).mapToDouble(t -> calcBursty(z, l, t)).toArray();
                hasConverged = hasConverged && IntStream
                        .range(0, LptaDocs.nTimeslots())
                        .allMatch(t -> Math.abs(newVal[t] - timeDistTopicLocs[z][l][t]) < Lpta.EPSILON);
                timeDistTopicLocs[z][l] = newVal;
            });
        });
    }

    private static double calcBursty(int z, int l, int t) {
        long tVal = LptaDocs.getTimestamp(t);
        return (NORM / stdDeviations[l][z]) * Math.exp(-(sqr(tVal - means[l][z]))/sqr(stdDeviations[l][z]));
    }

    private static double calcPeriodic(int z, int l, int t) {
        double p = periods[z];
        double intervalId = (int) (t / p);
        double probK = p / LptaDocs.nTimeslots(); // TODO: Is choice of K uniform? Yes if the number of docs per timestamp is constant?
        return probK * (NORM / stdDeviations[l][z]) * Math.exp(-sqr((double)t - means[l][z] - intervalId * p)/sqr(stdDeviations[l][z]));
    }

    // TODO: Should the timestamps be normalized? So that the first timestamp = 0?
    private static void updateMeans() {
        IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
            int[] docsInLoc = LptaDocs.getDocsInLoc(l);
            IntStream.range(0, nBurstyTopics+nPeriodicTopics).forEach(z -> {
                IntToDoubleFunction tsCalc;
                if (z < nPeriodicTopics)
                    tsCalc = (d) -> (LptaDocs.docs[d].getTimestampId()  - ((int) (LptaDocs.docs[d].getTimestampId() / periods[z])) * periods[z]) * calcAllWords(d, z);
                else
                    tsCalc = (d) -> LptaDocs.docs[d].getTimestamp() * calcAllWords(d, z);
                means[l][z] = calcNewVal(tsCalc, docsInLoc, z);
            });
        });
    }

    private static void updateStdDeviations() {
        IntStream.range(0, LptaDocs.nLocations()).forEach(l -> {
            int[] docsInLoc = LptaDocs.getDocsInLoc(l);
            IntStream.range(0, nBurstyTopics+nPeriodicTopics).forEach(z -> {
                IntToDoubleFunction tsCalc;
                if (z < nPeriodicTopics)
                    tsCalc = (d) -> sqr(LptaDocs.docs[d].getTimestamp() % (int) periods[z] - means[l][z]) * calcAllWords(d, z);
                else
                    tsCalc = (d) -> sqr(LptaDocs.docs[d].getTimestamp() - means[l][z]) * calcAllWords(d, z);
                stdDeviations[l][z] = Math.sqrt(calcNewVal(tsCalc, docsInLoc, z));
            });
        });
    }

    private static double calcNewVal(IntToDoubleFunction tsCalcF, int[] docsInLoc, int z) {
        double numerator = Arrays
                .stream(docsInLoc)
                .mapToDouble(tsCalcF)
                .sum();
        double denominator = Arrays.stream(docsInLoc).mapToDouble(d -> calcAllWords(d, z)).sum();
        return numerator / denominator;
    }

    private static double calcAllWords(int d, int z) {
        return IntStream
                .range(0, LptaDocs.nWords())
                .mapToDouble(w -> LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z))
                .sum();
    }

    private static double sqr(double v) { return v * v; }

    public static boolean hasConverged() {
        return hasConverged;
    }

    public static double[][][] getDistribution() {
        return timeDistTopicLocs;
    }

    public static double get(int z, int d) {
        if (z == nTopics) return backgroundTopic;
        Document doc = LptaDocs.docs[d];
        return timeDistTopicLocs[z][doc.getLocationId()][doc.getTimestampId()];
    }
}
