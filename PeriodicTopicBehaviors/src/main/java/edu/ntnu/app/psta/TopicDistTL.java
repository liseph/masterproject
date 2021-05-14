package edu.ntnu.app.psta;

import edu.ntnu.app.Algorithm;

import java.util.Arrays;

public class TopicDistTL {

    private static int nTopics;
    private static double[][][] topicDistributionTL;
    private static boolean converges;

    public static void initialize(int nTopics_) {
        nTopics = nTopics_;
        converges = false;
        topicDistributionTL = new double[PstaDocs.nLocations()][PstaDocs.nTimeslots()][];
        for (int l = 0; l < PstaDocs.nLocations(); l++) {
            for (int t = 0; t < PstaDocs.nTimeslots(); t++) {
                topicDistributionTL[l][t] = Algorithm.generateRandomDistribution(nTopics);
            }
        }
    }

    public static void update() {
        converges = true;
        for (int l = 0; l < PstaDocs.nLocations(); l++) {
            for (int t = 0; t < PstaDocs.nTimeslots(); t++) {
                double[] numerator = new double[nTopics];
                double denominator = 0;
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = calcForAllTLDocsAndWords(z, t, l);
                    denominator += numerator[z];
                }
                double uniform = 1.0 / nTopics;
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = denominator != 0 ? numerator[z] / denominator : uniform;
                    converges = converges && Math.abs(numerator[z] - topicDistributionTL[l][t][z]) < Psta.CONVERGES_LIM;
                }
                topicDistributionTL[l][t] = numerator;
            }
        }
    }

    private static double baseCalc(int z, int d, int w) {
        return PstaDocs.getWordCount(d, w) * LatentWordByTopic.get(d, w, z) * (1 - LatentWordByTL.get(d, w, z));
    }

    private static double baseCalcForAllWords(int z, int d) {
        return Arrays.stream(PstaDocs.getDoc(d).getTermIndices()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    private static double calcForAllTLDocsAndWords(int z, int t, int l) {
        return PstaDocs.getIndexOfDocsWithTL(t, l).mapToDouble(d -> baseCalcForAllWords(z, d)).sum();
    }

    public static double get(int locIndex, int timeIndex, int topicIndex) {
        return topicDistributionTL[locIndex][timeIndex][topicIndex];
    }

    public static void clear() {
        nTopics = 0;
        topicDistributionTL = null;
        converges = false;
    }

    public static boolean hasConverged() {
        return converges;
    }

    @Override
    public String toString() {
        return "\np(z|t,l){" +
                ", [l][t][z]=" + Arrays.deepToString(topicDistributionTL) +
                '}';
    }
}