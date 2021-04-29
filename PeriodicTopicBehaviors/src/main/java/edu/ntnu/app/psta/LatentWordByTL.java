package edu.ntnu.app.psta;

import java.util.Arrays;

public class LatentWordByTL implements Variable {

    private final double[][] latentWordByTL;
    private final VariableList topics;
    private final VariableList topicDistDocs;
    private final VariableList topicDistTLs;
    private final int docIndex;

    public LatentWordByTL(VariableList topics, VariableList topicDistDocs, VariableList topicDistTLs, int docIndex) {
        this.topics = topics;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
        this.docIndex = docIndex;
        this.latentWordByTL = new double[PstaDocs.nWords()][topics.length()];
        for (int i = 0; i < PstaDocs.nWords(); i++) {
            // No need for initial values as we update the latent variables first. Must set to 0 as default is null...
            this.latentWordByTL[i] = new double[topics.length()];
            Arrays.fill(this.latentWordByTL[i], 0f);
        }
    }

    public static VariableList generateEmptyTopicDist(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs) {
        Variable[] variables = new LatentWordByTL[PstaDocs.nDocuments()];
        for (int i = 0; i < PstaDocs.nDocuments(); i++) {
            variables[i] = new LatentWordByTL(themes, topicDistDocs, topicDistTLs, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int z = 0; z < topics.length(); z++) {
            for (int w = 0; w < PstaDocs.nWords(); w++) {
                // The first part, p(w|z), is not a part of the paper, but w is not included at all in the formula..
                //double numerator = topics.get(z).get(w) * PSTA.LAMBDA_TL * topicDistTLs
                double numerator = Psta.LAMBDA_TL * topicDistTLs
                        .get(PstaDocs.getDoc(docIndex).getLocationId())
                        .get(PstaDocs.getDoc(docIndex).getTimestampId(), z);
                double denominator = (1 - Psta.LAMBDA_TL) * topicDistDocs.get(docIndex).get(z) + numerator;
                double oldVal = latentWordByTL[w][z];
                double newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
                latentWordByTL[w][z] = newVal;
            }
        }
        return converges;
    }

    @Override
    public void setVars(VariableList p1, VariableList p2) {
        // Do nothing, it's a bit clumsy setup but oh well.
    }

    @Override
    public double get(int... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Wrong number of values passed to LatentWordByTopic.get(). It should be 2.");
        }
        int wordIndex = values[0];
        int topicIndex = values[1];
        return latentWordByTL[wordIndex][topicIndex];
    }
}