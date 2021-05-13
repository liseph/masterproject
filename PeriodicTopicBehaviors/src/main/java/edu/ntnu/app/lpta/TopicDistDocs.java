package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class TopicDistDocs {

    private static double[][] topicDistDoc;
    private static int nTopics;
    private static boolean hasConverged = false;

    public static void initialize(int nPeriodicTopics) {
        nTopics = nPeriodicTopics + 1;
        topicDistDoc = new double[LptaDocs.nDocuments()][nTopics];
        // double[] zs = IntStream.range(0, nTopics).mapToDouble(i -> 1.0 / nTopics).toArray();
        IntStream.range(0, LptaDocs.nDocuments()).forEach(d -> {
            double[] zss = new Random().doubles(nTopics, 0, 1).toArray();
            double sum = Arrays.stream(zss).sum();
            double[] zs = Arrays.stream(zss).map(z -> z / sum).toArray();
            topicDistDoc[d] = zs;
        });

    }

    public static void update() {
        hasConverged = true;
        IntStream.range(0, LptaDocs.nDocuments()).forEach(d -> {
            double[] numerator = IntStream.range(0, nTopics).mapToDouble(z -> calcAllWords(d, z)).toArray();
            double denominator = Arrays.stream(numerator).sum();
            if (denominator == 0) System.out.println("NaN TopicDistDocs");
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

    public static void clear() {
    }
}
