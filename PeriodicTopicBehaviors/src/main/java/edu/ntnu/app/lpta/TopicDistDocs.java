package edu.ntnu.app.lpta;

import java.util.Arrays;

public class TopicDistDocs {

    private static double[][] topicDistDoc;
    private static int nTopics;
    private static boolean converges = false;

    public static void initialize(int nPeriodicTopics) {
        nTopics = nPeriodicTopics + 1;
        topicDistDoc = new double[LptaDocs.nDocuments()][nTopics];
        double[] zs = new double[nTopics];
        Arrays.fill(zs, 1.0 / nTopics);
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            topicDistDoc[d] = zs;
        }
    }

    public static void update() {
        converges = true;
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            double[] numerator = new double[nTopics];
            double denominator = 0;
            for (int z = 0; z < nTopics; z++) {
                numerator[z] = calcAllWords(d, z);
                denominator += numerator[z];
            }
            double uniform = 1.0 / nTopics;
            for (int z = 0; z < nTopics; z++) {
                numerator[z] = denominator != 0 ? numerator[z] / denominator : uniform;
                converges = converges && Math.abs(numerator[z] - topicDistDoc[d][z]) < Lpta.CONVERGES_LIM;
            }
            topicDistDoc[d] = numerator;
        }
    }

    private static double calcAllWords(int d, int z) {
        return Arrays.stream(LptaDocs.getDoc(d).getTermIndices()).mapToDouble(w -> calc(d, w, z)).sum();
    }

    private static double calc(int d, int w, int z) {
        return LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z);
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static double[][] getDistribution() {
        return topicDistDoc;
    }

    public static double get(int d, int z) {
        return topicDistDoc[d][z];
    }

    public static void clear() {
        topicDistDoc = null;
        nTopics = 0;
        converges = false;
    }
}
