package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistDoc implements Variable {

    private final int docIndex;
    private final Float[] topicDistributionDoc;
    private final int nTopics;
    private VariableList latentWordByTopic;
    private VariableList latentWordByTL;

    public TopicDistDoc(int nTopics, int docIndex) {
        this.nTopics = nTopics;
        this.docIndex = docIndex;
        this.topicDistributionDoc = VariableList.generateRandomDistribution(nTopics);
    }

    public static VariableList generateEmptyTopicDist(int nTopics) {
        Variable[] variables = new TopicDistDoc[Docs.nDocuments()];
        for (int i = 0; i < Docs.nDocuments(); i++) {
            variables[i] = new TopicDistDoc(nTopics, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        Float denominator = IntStream.range(0, nTopics).mapToObj(z2 -> baseCalcForAllWords(z2, docIndex)).reduce(0f, Float::sum);
        for (int z = 0; z < nTopics; z++) {
            Float numerator = baseCalcForAllWords(z, docIndex);
            Float oldVal = topicDistributionDoc[z];
            Float newVal = denominator != 0 ? numerator / denominator : 0;
            converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
            topicDistributionDoc[z] = newVal;
        }
        return converges;
    }

    private Float baseCalc(int z, int d, int w) {
        return Docs.getWordCount(d, w) * latentWordByTopic.get(d).get(w, z) * (1 - latentWordByTL.get(d).get(w, z));
    }

    private Float baseCalcForAllWords(int z, int d) {
        int[] termIndices = Docs.get(d).getTermIndices();
        return Arrays.stream(Docs.get(d).getTermIndices()).mapToObj(w -> baseCalc(z, d, w)).reduce(0f, Float::sum);
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopic = latentWordByTopic;
        this.latentWordByTL = latentWordByTL;
    }

    @Override
    public Float get(int... topicIndex) {
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
