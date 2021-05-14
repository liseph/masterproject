package edu.ntnu.app.psta;

import edu.ntnu.app.Algorithm;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistDoc {

    private static double[][] topicDistributionDoc;
    private static int nTopics;
    private static boolean converges;

    public static void initialize(int nTopics_) {
        nTopics = nTopics_;
        converges = false;
        topicDistributionDoc = new double[PstaDocs.nDocuments()][];
        for (int i = 0; i < PstaDocs.nDocuments(); i++) {
            topicDistributionDoc[i] = Algorithm.generateRandomDistribution(nTopics);
        }
    }

    public static void update() {
        converges = true;
        for (int d = 0; d < PstaDocs.nDocuments(); d++) {
            double[] numerator = new double[nTopics];
            double denominator = 0;
            for (int z = 0; z < nTopics; z++) {
                numerator[z] = baseCalcForAllWords(z, d);
                denominator += numerator[z];
            }
            double uniform = 1.0 / nTopics;
            for (int z = 0; z < nTopics; z++) {
                numerator[z] = denominator != 0 ? numerator[z] / denominator : uniform;
                converges = converges && Math.abs(numerator[z] - topicDistributionDoc[d][z]) < Psta.CONVERGES_LIM;
            }
            topicDistributionDoc[d] = numerator;
        }
    }

    private static double baseCalc(int z, int d, int w) {
        return PstaDocs.getWordCount(d, w) * LatentWordByTopic.get(d, w, z) * (1 - LatentWordByTL.get(d, w, z));
    }

    private static double baseCalcForAllWords(int z, int d) {
        return Arrays.stream(PstaDocs.getDoc(d).getTermIndices()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    public static double get(int docIndex, int topicIndex) {
        return topicDistributionDoc[docIndex][topicIndex];
    }

    public static void clear() {
        topicDistributionDoc = null;
        nTopics = 0;
        converges = false;
    }

    public static boolean hasConverged() {
        return converges;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        IntStream.range(0, PstaDocs.nDocuments()).forEach(d -> {
            builder.append("p(z|d){d=");
            builder.append(d);
            builder.append(", [z]=");
            builder.append(Arrays.toString(topicDistributionDoc));
            builder.append("}\n");
        });
        return builder.toString();
    }
}
