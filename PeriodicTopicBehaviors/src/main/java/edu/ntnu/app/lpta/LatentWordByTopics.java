package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class LatentWordByTopics {

    private static double[][][] latentWordByTopics;
    private static boolean hasConverged = false;
    private static int nTopics;

    public static void initialize(int nPeriodicTopics) {
        nTopics = nPeriodicTopics + 1;
        latentWordByTopics = new double[LptaDocs.nDocuments()][][];
        IntStream.range(0, LptaDocs.nDocuments()).forEach(d -> {
            int termIndices = LptaDocs.getDoc(d).getTermIndices().length;
            latentWordByTopics[d] = new double[termIndices][nTopics];
        });
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
                double[] newVals;
                if (denominator != 0)
                    newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
                else {
                    System.out.println("NaN LatentWordByTopics");
                    newVals = IntStream.range(0, nTopics).mapToDouble(i -> ((double) i) / nTopics).toArray();
                }
                if (hasConverged) {
                    hasConverged = IntStream
                            .range(0, nTopics)
                            .allMatch(z -> Math.abs(newVals[z] - latentWordByTopics[d][wIndex][z]) < Lpta.CONVERGES_LIM);
                }
                latentWordByTopics[d][wIndex] = newVals;
            });
        });
    }

    private static double calc(int d, int w, int z) {
        return TimeDistTopicLocs.get(z, d) * Topics.get(z, w) * TopicDistDocs.get(d, z);
    }

    public static double get(int d, int w, int z) {
        int[] termIndices = LptaDocs.getDoc(d).getTermIndices();
        int wIndex = IntStream.range(0, termIndices.length).filter(i -> termIndices[i] == w).findFirst().orElse(-1);
        if (wIndex == -1) return 0;
        return latentWordByTopics[d][wIndex][z];
    }
}
