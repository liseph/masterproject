package edu.ntnu.app.psta;

import java.util.stream.IntStream;

public class TopicDistTL implements Variable {

    private final Documents docs;
    private final int themeIndex;
    private final int nTopics;
    private final double[][] topicDistributionTL;
    private VariableList latentWordByTopics;
    private VariableList latentWordByTLs;

    public TopicDistTL(int nTopics, int themeIndex, Documents docs) {
        this.docs = docs;
        this.nTopics = nTopics;
        this.themeIndex = themeIndex;
        this.topicDistributionTL = new double[docs.nTimeslots()][docs.nLocations()];
        for (int i = 0; i < docs.nTimeslots(); i++) {
            this.topicDistributionTL[i] = VariableList.generateRandomDistribution(docs.nLocations());
        }
    }

    public static VariableList generateEmptyTopicDist(int nTopics, Documents docs) {
        Variable[] variables = new TopicDistTL[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new TopicDistTL(nTopics, i, docs);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int t = 0; t < docs.nTimeslots(); t++) {
            for (int l = 0; l < docs.nTimeslots(); l++) {
                double numerator = calcForAllTLDocsAndWords(themeIndex, t, l);
                int finalL = l; // To use in stream
                int finalT = t; // To use in stream
                double denominator = IntStream.range(0, nTopics).mapToDouble(z -> calcForAllTLDocsAndWords(z, finalT, finalL)).sum();
                double oldVal = topicDistributionTL[t][l];
                double newVal = numerator / denominator;
                converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
                topicDistributionTL[t][l] = newVal;
            }
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return docs.getWordCount(d, w) * latentWordByTopics.get(z).get(d, w) * (1 - latentWordByTLs.get(z).get(d, w));
    }

    private double baseCalcForAllWords(int z, int d) {
        return IntStream.range(0, docs.nWords()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    private double calcForAllTLDocsAndWords(int z, int t, int l) {
        return docs.getIndexOfDocsWithTL(t, l).mapToDouble(d -> baseCalcForAllWords(z, d)).sum();
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
        return topicDistributionTL[values[0]][values[1]];
    }
}