package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.IntStream;

public class LatentWordByTopic implements Variable {

    private final VariableList themes;
    private final VariableList topicDistDocs;
    private final VariableList topicDistTLs;
    private final int docIndex;
    private final Float[][] latentWordByTopic;

    public LatentWordByTopic(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, int docIndex) {
        this.themes = themes;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
        this.docIndex = docIndex;
        this.latentWordByTopic = new Float[PstaDocs.nWords()][themes.length()];
        for (int i = 0; i < PstaDocs.nWords(); i++) {
            // No need for initial values as we update the latent variables first. Must set to 0 as default is null...
            this.latentWordByTopic[i] = new Float[themes.length()];
            Arrays.fill(this.latentWordByTopic[i], 0f);
        }
    }

    public static VariableList generateEmptyTopicDist(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs) {
        int nDocs = PstaDocs.nDocuments();
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
            for (int w = 0; w < PstaDocs.nWords(); w++) {
                Float numerator = (1 - Psta.LAMBDA_B) * calcProbTopicByDocAndTL(z, docIndex, w);
                int finalW = w; // To use in stream
                Float denominator = Psta.LAMBDA_B * PstaDocs.backgroundTheme[w] +
                        (1 - Psta.LAMBDA_B) * IntStream
                                .range(0, themes.length())
                                .mapToObj(z2 -> calcProbTopicByDocAndTL(z2, docIndex, finalW))
                                .reduce(0f, Float::sum);
                Float oldVal = latentWordByTopic[w][z];
                Float newVal = denominator != 0 ? numerator / denominator : 0;
                converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
                latentWordByTopic[w][z] = newVal;
            }
        }
        return converges;
    }

    private Float calcProbTopicByDocAndTL(int z, int d, int w) {
        return themes.get(z).get(w) *
                ((1 - Psta.LAMBDA_TL) * topicDistDocs.get(d).get(z) +
                        Psta.LAMBDA_TL *
                                topicDistTLs.get(PstaDocs.get(d).getLocationId()).get(PstaDocs.get(d).getTimestampId(), z));
    }

    @Override
    public void setVars(VariableList p1, VariableList p2) {
        // Do nothing, it's a bit clumsy setup but oh well.
    }

    @Override
    public Float get(int... values) {
        if (values.length != 2) {
            throw new IllegalArgumentException("Wrong number of values passed to LatentWordByTopic.get(). It should be 2.");
        }
        int wordIndex = values[0];
        int topicIndex = values[1];
        return latentWordByTopic[wordIndex][topicIndex];
    }
}
