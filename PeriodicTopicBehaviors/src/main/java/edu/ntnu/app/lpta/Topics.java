package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Topics {

    enum TopicType {
        PERIODIC,
        BURSTY,
        BACKGROUND
    }

    private static double[][] topics; // Periodic topics, bursty topics and then background topic.
    private static boolean hasConverged = false;
    private static int nPeriodicTopics;
    private static int nBurstyTopics;
    private static int nTopics;

    public static void initialize(int nPeriodicTops, int nBurstyTops) {
        if (!LatentWordByTopics.isInitialized()) {
            throw new IllegalStateException("Cannot initialize Topics before LatentWordByTopics.");
        }
        nTopics = nPeriodicTops + nBurstyTops + 1;
        nBurstyTopics = nBurstyTops;
        nPeriodicTopics = nPeriodicTops;
        topics = new double[nTopics][LptaDocs.nWords()];
        update();
    }

    public static void update() {
        hasConverged = true;
        IntStream.range(0, nTopics).forEach(z -> {
            double[] numerator = IntStream.range(0, LptaDocs.nWords()).mapToDouble(w -> calcAllDocs(w, z)).toArray();
            double denominator = Arrays.stream(numerator).sum();
            double[] newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
            hasConverged = hasConverged && IntStream
                    .range(0, LptaDocs.nWords())
                    .allMatch(w -> Math.abs(newVals[w] - topics[z][w]) < Lpta.EPSILON);
            topics[z] = newVals;
        });
    }

    private static double calcAllDocs(int w, int z) {
        // TODO Wednesday: v is NaN in second call to update, investigate
        double v = IntStream.range(0, LptaDocs.nDocuments()).mapToDouble(d -> calc(d, w, z)).sum();
        return v;
    }

    private static double calc(int d, int w, int z) {
        return LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z);
    }

    public static boolean hasConverged() {
        return hasConverged;
    }

    public static double[][] getDistribution() {
        return topics;
    }

    public static TopicType getTopicType(int z) {
        return z < nPeriodicTopics ? TopicType.PERIODIC :
                z < nPeriodicTopics + nBurstyTopics ? TopicType.BURSTY : TopicType.BACKGROUND;
    }

    public static double get(int z, int w) {
        return topics[z][w];
    }
}
