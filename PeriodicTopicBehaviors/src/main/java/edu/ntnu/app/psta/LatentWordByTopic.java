package edu.ntnu.app.psta;

import java.util.Arrays;
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
        this.latentWordByTopic = new double[PstaDocs.nWords()][themes.length()];
        for (int i = 0; i < PstaDocs.nWords(); i++) {
            // No need for initial values as we update the latent variables first. Must set to 0 as default is null...
            this.latentWordByTopic[i] = new double[themes.length()];
            Arrays.fill(this.latentWordByTopic[i], 0);
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
        for (int w = 0; w < PstaDocs.nWords(); w++) {
            int finalW = w; // To use in stream
            double[] base = IntStream.range(0, themes.length())
                    .mapToDouble(z2 -> calcProbTopicByDocAndTL(z2, docIndex, finalW))
                    .toArray();
            double denominator = Psta.LAMBDA_B * PstaDocs.backgroundTheme[w] +
                    (1 - Psta.LAMBDA_B) * Arrays.stream(base).sum();
            double[] newVal = Arrays.stream(base).map(val -> (1 - Psta.LAMBDA_B) * val / denominator).toArray();
            converges = converges && IntStream.range(0, themes.length()).allMatch(z -> Math.abs(newVal[z] - latentWordByTopic[finalW][z]) < Psta.EPSILON);
            latentWordByTopic[w] = newVal;
        }
        return converges;
    }

    private double calcProbTopicByDocAndTL(int z, int d, int w) {
        return themes.get(z).get(w) *
                ((1 - Psta.LAMBDA_TL) * topicDistDocs.get(d).get(z) +
                        Psta.LAMBDA_TL *
                                topicDistTLs.get(PstaDocs.getDoc(d).getLocationId()).get(PstaDocs.getDoc(d).getTimestampId(), z));
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
