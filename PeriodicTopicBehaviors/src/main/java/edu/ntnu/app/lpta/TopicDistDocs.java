package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistDocs {

    private static double[][] topicDistDoc;
    private static int nTopics;
    private static boolean hasConverged = false;

    public static void initialize(int nPeriodicTopics) {
        if (!LatentWordByTopics.isInitialized()) {
            throw new IllegalStateException("Cannot initialize TopicDistDocs before LatentWordByTopics.");
        }
        nTopics = nPeriodicTopics + 1;
        topicDistDoc = new double[LptaDocs.nDocuments()][nTopics];
        update();

    }

    public static void update() {
        hasConverged = true;
        IntStream.range(0, LptaDocs.nDocuments()).forEach(d -> {
            double[] numerator = IntStream.range(0, nTopics).mapToDouble(z -> calcAllWords(d, z)).toArray();
            double denominator = Arrays.stream(numerator).sum();
            double[] newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
            hasConverged = hasConverged && IntStream
                    .range(0, nTopics)
                    .allMatch(z -> Math.abs(newVals[z] - topicDistDoc[d][z]) < Lpta.CONVERGES_LIM);
            topicDistDoc[d] = newVals;
        });
    }

    private static double calcAllWords(int d, int z) {
        return Arrays.stream(LptaDocs.getDoc(d).getTermIndices()).mapToDouble(w -> calc(d, w, z)).sum();
    }

    private static double calc(int d, int w, int z) {
        return LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z);
    }

    public static boolean hasConverged() {
        return hasConverged;
    }

    public static double[][] getDistribution() {
        return topicDistDoc;
    }

    public static double get(int d, int z) {
        return topicDistDoc[d][z];
    }
}
