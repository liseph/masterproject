package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistTL implements Variable {

    private final int timeIndex;
    private final int nTopics;
    private final double[][] topicDistributionTL;
    private VariableList latentWordByTopics;
    private VariableList latentWordByTLs;

    public TopicDistTL(int nTopics, int timeIndex) {
        this.nTopics = nTopics;
        this.timeIndex = timeIndex;
        this.topicDistributionTL = new double[Docs.nLocations()][nTopics];
        for (int i = 0; i < Docs.nLocations(); i++) {
            this.topicDistributionTL[i] = VariableList.generateRandomDistribution(nTopics);
        }
    }

    public static VariableList generateEmptyTopicDist(int nTopics) {
        Variable[] variables = new TopicDistTL[Docs.nTimeslots()];
        for (int i = 0; i < Docs.nTimeslots(); i++) {
            variables[i] = new TopicDistTL(nTopics, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int l = 0; l < Docs.nLocations(); l++) {
            int finalL = l; // To use in stream
            double denominator = IntStream.range(0, nTopics).mapToDouble(z2 -> calcForAllTLDocsAndWords(z2, timeIndex, finalL)).sum();
            for (int z = 0; z < nTopics; z++) {
                double numerator = calcForAllTLDocsAndWords(z, timeIndex, l);
                double oldVal = topicDistributionTL[l][z];
                double newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
                topicDistributionTL[l][z] = newVal;
            }
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return Docs.getWordCount(d, w) * latentWordByTopics.get(d).get(w, z) * (1 - latentWordByTLs.get(d).get(w, z));
    }

    private double baseCalcForAllWords(int z, int d) {
        return Arrays.stream(Docs.get(d).getTermIndices()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    private double calcForAllTLDocsAndWords(int z, int t, int l) {
        return Docs.getIndexOfDocsWithTL(t, l).mapToDouble(d -> baseCalcForAllWords(z, d)).sum();
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopics = latentWordByTopic;
        this.latentWordByTLs = latentWordByTL;
    }

    @Override
    public double get(int... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Wrong number of values passed to TopicDistTL.get(). It should be 2.");
        }
        int locationIndex = values[0];
        int topicIndex = values[1];
        return topicDistributionTL[locationIndex][topicIndex];
    }

    @Override
    public String toString() {
        return "\np(z|t,l){" +
                "t=" + timeIndex +
                ", [l][z]=" + Arrays.deepToString(topicDistributionTL) +
                '}';
    }
}