package edu.ntnu.app.psta;

import java.util.stream.IntStream;

public class LatentWordByTopic implements Variable {

    private final VariableList themes;
    private final VariableList topicDistDocs;
    private final VariableList topicDistTLs;
    private final int docIndex;
    private final double[][] latentWordByTopic;

    public LatentWordByTopic(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, int docIndex) {
        this.themes = themes;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
        this.docIndex = docIndex;
        this.latentWordByTopic = new double[Docs.nWords()][themes.length()];
        for (int i = 0; i < Docs.nWords(); i++) {
            // No need for initial values as we update the latent variables first.
            this.latentWordByTopic[i] = new double[themes.length()];
        }
    }

    public static VariableList generateEmptyTopicDist(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs) {
        int nDocs = Docs.nDocuments();
        Variable[] variables = new LatentWordByTopic[nDocs];
        for (int i = 0; i < nDocs; i++) {
            variables[i] = new LatentWordByTopic(themes, topicDistDocs, topicDistTLs, i);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int z = 0; z < themes.length(); z++) {
            for (int w = 0; w < Docs.nWords(); w++) {
                double numerator = (1 - PSTA.LAMBDA_B) * calcProbTopicByDocAndTL(z, docIndex, w);
                int finalW = w; // To use in stream
                double denominator = PSTA.LAMBDA_B * Docs.backgroundTheme[w] +
                        (1 - PSTA.LAMBDA_B) * IntStream
                                .range(0, themes.length())
                                .mapToDouble(z2 -> calcProbTopicByDocAndTL(z2, docIndex, finalW))
                                .sum();
                double oldVal = latentWordByTopic[w][z];
                double newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
                latentWordByTopic[w][z] = newVal;
            }
        }
        return converges;
    }

    private double calcProbTopicByDocAndTL(int z, int d, int w) {
        return themes.get(z).get(w) *
                ((1 - PSTA.LAMBDA_TL) * topicDistDocs.get(d).get(z) +
                        PSTA.LAMBDA_TL *
                                topicDistTLs.get(Docs.get(d).getTimestampId()).get(Docs.get(d).getLocationId(), z));
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
        return latentWordByTopic[wordIndex][topicIndex];
    }
}
