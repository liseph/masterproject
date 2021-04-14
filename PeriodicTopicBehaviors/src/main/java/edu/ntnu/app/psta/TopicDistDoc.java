package edu.ntnu.app.psta;

import java.util.stream.IntStream;

public class TopicDistDoc implements Variable {

    private final Documents docs;
    private final int themeIndex;
    private final double[] topicDistributionDoc;
    private final int nTopics;
    private VariableList latentWordByTopic;
    private VariableList latentWordByTL;

    public TopicDistDoc(int nTopics, int themeIndex, Documents docs) {
        this.docs = docs;
        this.nTopics = nTopics;
        this.themeIndex = themeIndex;
        this.topicDistributionDoc = VariableList.generateRandomDistribution(docs.nDocuments());
    }

    public static VariableList generateEmptyTopicDist(int nTopics, Documents docs) {
        Variable[] variables = new TopicDistDoc[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new TopicDistDoc(nTopics, i, docs);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int d = 0; d < docs.nDocuments(); d++) {
            double numerator = baseCalcForAllWords(themeIndex, d);
            int finalD = d; // To use in stream
            double denominator = IntStream.range(0, nTopics).mapToDouble(z -> baseCalcForAllWords(z, finalD)).sum();
            double oldVal = topicDistributionDoc[d];
            double newVal = numerator / denominator;
            converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
            topicDistributionDoc[d] = newVal;
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return docs.getWordCount(d, w) * latentWordByTopic.get(z).get(d, w) * (1 - latentWordByTL.get(z).get(d, w));
    }

    private double baseCalcForAllWords(int z, int d) {
        return IntStream.range(0, docs.nWords()).mapToDouble(w -> baseCalc(z, d, w)).sum();
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopic = latentWordByTopic;
        this.latentWordByTL = latentWordByTL;
    }

    @Override
    public double get(int... docIndex) {
        if (docIndex.length != 1) {
            throw new IllegalArgumentException("Wrong number of values passed to TopicDistDoc.get(). It should be 1.");
        }
        return topicDistributionDoc[docIndex[0]];
    }
}
