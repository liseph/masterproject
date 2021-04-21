package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TopicDistTL implements Variable {

    private final int locationIndex;
    private final int nTopics;
    private final Float[][] topicDistributionTL;
    private VariableList latentWordByTopics;
    private VariableList latentWordByTLs;

    public TopicDistTL(int nTopics, int locationIndex) {
        this.nTopics = nTopics;
        this.locationIndex = locationIndex;
        this.topicDistributionTL = new Float[PstaDocs.nTimeslots()][nTopics];
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
            Float denominator = IntStream.range(0, nTopics).mapToObj(z2 -> calcForAllTLDocsAndWords(z2, finalT, locationIndex)).reduce(0f, Float::sum);
            for (int z = 0; z < nTopics; z++) {
                Float numerator = calcForAllTLDocsAndWords(z, t, locationIndex);
                Float oldVal = topicDistributionTL[t][z];
                Float newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
                topicDistributionTL[t][z] = newVal;
            }
        }
        return converges;
    }

    private Float baseCalc(int z, int d, int w) {
        return PstaDocs.getWordCount(d, w) * latentWordByTopics.get(d).get(w, z) * (1 - latentWordByTLs.get(d).get(w, z));
    }

    private Float baseCalcForAllWords(int z, int d) {
        return Arrays.stream(PstaDocs.get(d).getTermIndices()).mapToObj(w -> baseCalc(z, d, w)).reduce(0f, Float::sum);
    }

    private Float calcForAllTLDocsAndWords(int z, int t, int l) {
        return PstaDocs.getIndexOfDocsWithTL(t, l).map(d -> baseCalcForAllWords(z, d)).reduce(0f, Float::sum);
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopics = latentWordByTopic;
        this.latentWordByTLs = latentWordByTL;
    }

    @Override
    public Float get(int... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Wrong number of values passed to TopicDistTL.get(). It should be 2.");
        }
        int timeIndex = values[0];
        int topicIndex = values[1];
        return topicDistributionTL[timeIndex][topicIndex];
    }

    public Float[][] getTopicDistributionTL() {
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