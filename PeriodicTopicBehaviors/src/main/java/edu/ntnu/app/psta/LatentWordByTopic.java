package edu.ntnu.app.psta;

import java.util.stream.IntStream;

public class LatentWordByTopic implements Variable {

    private final Documents docs;
    private final VariableList themes;
    private final VariableList topicDistDocs;
    private final VariableList topicDistTLs;
    private final int themeIndex;
    private final double[][] latentWordByTopic;

    public LatentWordByTopic(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, int themeIndex, Documents docs) {
        this.docs = docs;
        this.themes = themes;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
        this.themeIndex = themeIndex;
        this.latentWordByTopic = new double[docs.nDocuments()][docs.nWords()];
        for (int i = 0; i < docs.nDocuments(); i++) {
            this.latentWordByTopic[i] = VariableList.generateRandomDistribution(docs.nWords());
        }
    }

    public static VariableList generateEmptyTopicDist(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, Documents docs) {
        int nTopics = themes.length();
        Variable[] variables = new LatentWordByTopic[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new LatentWordByTopic(themes, topicDistDocs, topicDistTLs, i, docs);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int d = 0; d < docs.nDocuments(); d++) {
            for (int w = 0; w < docs.nWords(); w++) {
                double numerator = (1 - PSTA.LambdaB) * calcProbTopicByDocAndTL(themeIndex, d, w);
                int finalD = d; // To use in stream
                int finalW = w; // To use in stream
                double denominator = PSTA.LambdaB * docs.backgroundTheme[w] +
                        (1 - PSTA.LambdaB) * IntStream
                                .range(0, themes.length())
                                .mapToDouble(z -> calcProbTopicByDocAndTL(z, finalD, finalW))
                                .sum();
                double oldVal = latentWordByTopic[d][w];
                double newVal = numerator / denominator;
                converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
                latentWordByTopic[d][w] = newVal;
            }
        }
        return converges;
    }

    private double calcProbTopicByDocAndTL(int z, int d, int w) {
        return themes.get(z).get(w) *
                ((1 - PSTA.LambdaTL) * topicDistDocs.get(z).get(d) +
                        PSTA.LambdaTL *
                                topicDistTLs.get(z).get(docs.get(d).getTimestampId(), docs.get(d).getLocationId()));
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
        int docIndex = values[0];
        int wordIndex = values[1];
        return latentWordByTopic[docIndex][wordIndex];
    }
}
