package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistTL implements Variable {

    private final int locationIndex;
    private final int nTopics;
    private final double[][] topicDistributionTL;
    private VariableList latentWordByTopics;
    private VariableList latentWordByTLs;

    public TopicDistTL(int nTopics, int locationIndex) {
        this.nTopics = nTopics;
        this.locationIndex = locationIndex;
        this.topicDistributionTL = new double[PstaDocs.nTimeslots()][nTopics];
        for (int i = 0; i < PstaDocs.nTimeslots(); i++) {
            this.topicDistributionTL[i] = VariableList.generateRandomDistribution(nTopics);
        }
    }

    public static VariableList generateEmptyTopicDist(int nTopics) {
        Variable[] variables = new TopicDistTL[PstaDocs.nLocations()];
        for (int i = 0; i < PstaDocs.nLocations(); i++) {
            variables[i] = new TopicDistTL(nTopics, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int t = 0; t < PstaDocs.nTimeslots(); t++) {
            int finalT = t; // To use in stream
            double denominator = IntStream.range(0, nTopics).mapToDouble(z2 -> calcForAllTLDocsAndWords(z2, finalT, locationIndex)).sum();
            for (int z = 0; z < nTopics; z++) {
                double numerator = calcForAllTLDocsAndWords(z, t, locationIndex);
                double oldVal = topicDistributionTL[t][z];
                double newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
                topicDistributionTL[t][z] = newVal;
            }
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return PstaDocs.getWordCount(d, w) * latentWordByTopics.get(d).get(w, z) * (1 - latentWordByTLs.get(d).get(w, z));
    }

    private double baseCalcForAllWords(int z, int d) {
        return Arrays.stream(PstaDocs.get(d).getTermIndices()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    private double calcForAllTLDocsAndWords(int z, int t, int l) {
        return PstaDocs.getIndexOfDocsWithTL(t, l).mapToDouble(d -> baseCalcForAllWords(z, d)).sum();
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
        int timeIndex = values[0];
        int topicIndex = values[1];
        return topicDistributionTL[timeIndex][topicIndex];
    }

    public double[][] getTopicDistributionTL() {
        return topicDistributionTL;
    }

    @Override
    public String toString() {
        return "\np(z|t,l){" +
                "l=" + locationIndex +
                ", [t][z]=" + Arrays.deepToString(topicDistributionTL) +
                '}';
    }
}