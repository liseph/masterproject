package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistDoc implements Variable {

    private final int docIndex;
    private final double[] topicDistributionDoc;
    private final int nTopics;
    private VariableList latentWordByTopic;
    private VariableList latentWordByTL;

    public TopicDistDoc(int nTopics, int docIndex) {
        this.nTopics = nTopics;
        this.docIndex = docIndex;
        this.topicDistributionDoc = VariableList.generateRandomDistribution(nTopics);
    }

    public static VariableList generateEmptyTopicDist(int nTopics) {
        Variable[] variables = new TopicDistDoc[PstaDocs.nDocuments()];
        for (int i = 0; i < PstaDocs.nDocuments(); i++) {
            variables[i] = new TopicDistDoc(nTopics, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        double denominator = IntStream.range(0, nTopics).mapToDouble(z2 -> baseCalcForAllWords(z2, docIndex)).sum();
        for (int z = 0; z < nTopics; z++) {
            double numerator = baseCalcForAllWords(z, docIndex);
            double oldVal = topicDistributionDoc[z];
            double newVal = denominator != 0 ? numerator / denominator : 0;
            converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
            topicDistributionDoc[z] = newVal;
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return PstaDocs.getWordCount(d, w) * latentWordByTopic.get(d).get(w, z) * (1 - latentWordByTL.get(d).get(w, z));
    }

    private double baseCalcForAllWords(int z, int d) {
        return Arrays.stream(PstaDocs.get(d).getTermIndices()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopic = latentWordByTopic;
        this.latentWordByTL = latentWordByTL;
    }

    @Override
    public double get(int... topicIndex) {
        if (topicIndex.length != 1) {
            throw new IllegalArgumentException("Wrong number of values passed to TopicDistDoc.get(). It should be 1.");
        }
        return topicDistributionDoc[topicIndex[0]];
    }

    @Override
    public String toString() {
        return "\np(z|d){" +
                "d=" + docIndex +
                ", [z]=" + Arrays.toString(topicDistributionDoc) +
                '}';
    }
}
