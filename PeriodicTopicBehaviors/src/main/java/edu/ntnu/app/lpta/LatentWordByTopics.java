package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class LatentWordByTopics {

    private static double[][][] latentWordByTopics;
    private static boolean isInitialized = false;
    private static boolean hasConverged = false;
    private static int nTopics;

    public static void initialize(int nPeriodicTopics) {
        if (isInitialized) return;
        nTopics = nPeriodicTopics + 1;
        latentWordByTopics = new double[LptaDocs.nDocuments()][][];
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            int nWordsInDoc = LptaDocs.getDoc(d).getTermIndices().length;
            latentWordByTopics[d] = new double[nWordsInDoc][];
            for (int w = 0; w < nWordsInDoc; w++) {
                double[] zs = new Random(1000).doubles(nTopics, 0, 1).toArray();
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
            int[] termIndices = LptaDocs.getDoc(d).getTermIndices();
            IntStream.range(0, termIndices.length).forEach(wIndex -> {
                int w = termIndices[wIndex];
                double[] numerator = IntStream.range(0, nTopics).mapToDouble(z -> calc(d, w, z)).toArray();
                double denominator = Arrays.stream(numerator).sum();
                double[] newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
                if (hasConverged) {
                    hasConverged = IntStream
                            .range(0, nTopics)
                            .allMatch(z -> Math.abs(newVals[z] - latentWordByTopics[d][wIndex][z]) < Lpta.EPSILON);
                }
                latentWordByTopics[d][wIndex] = newVals;
            });
        });
    }

    private static double calc(int d, int w, int z) {
        double v = TimeDistTopicLocs.get(z, d) * Topics.get(z, w) * TopicDistDocs.get(d, z);
        return v;
    }

    public static double get(int d, int w, int z) {
        int[] termIndices = LptaDocs.getDoc(d).getTermIndices();
        int wIndex = IntStream.range(0, termIndices.length).filter(i -> termIndices[i] == w).findFirst().orElse(-1);
        if (wIndex == -1) return 0;
        return latentWordByTopics[d][wIndex][z];
    }
}
