package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class LatentWordByTopics {

    private static double[][][] latentWordByTopics;
    private static boolean isInitialized = false;
    private static boolean hasConverged = false;
    private static int nTopics;

    public static void initialize(int nPeriodicTopics, int nBurstyTopics) {
        if (isInitialized) return;
        nTopics = nPeriodicTopics + nBurstyTopics + 1;
        latentWordByTopics = new double[LptaDocs.nDocuments()][LptaDocs.nWords()][nTopics];
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            for (int w = 0; w < LptaDocs.nWords(); w++) {
                double[] zs = new Random().doubles(nTopics, 0, 1).toArray();
                double total = Arrays.stream(zs).sum();
                latentWordByTopics[d][w] = Arrays.stream(zs).map(v -> v / total).toArray();
            }
        }
        isInitialized = true;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static boolean hasConverged() {
        return hasConverged;
    }

    public static void update() {
        hasConverged = true;
        IntStream.range(0, LptaDocs.nDocuments()).forEach(d -> {
            IntStream.range(0, LptaDocs.nWords()).forEach(w -> {
                double[] numerator = IntStream.range(0, nTopics).mapToDouble(z -> calc(d, w, z)).toArray();
                double denominator = Arrays.stream(numerator).sum();
                if (denominator == 0) {
                    System.out.println("null");
                }
                double[] newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
                hasConverged = hasConverged && IntStream
                        .range(0, nTopics)
                        .allMatch(z -> Math.abs(newVals[z] - latentWordByTopics[d][w][z]) < Lpta.EPSILON);
                latentWordByTopics[d][w] = newVals;
            });
        });
    }

    private static double calc(int d, int w, int z) {
        return TimeDistTopicLocs.get(z, d) * Topics.get(z, w) * TopicDistDocs.get(d, z);
    }

    public static double get(int d, int w, int z) {
        return latentWordByTopics[d][w][z];
    }
}
