package edu.ntnu.app.psta;

public class LatentWordByTL implements Variable {

    private final Documents docs;
    private final double[][] latentWordByTL;
    private final VariableList themes;
    private final VariableList topicDistDocs;
    private final VariableList topicDistTLs;
    private final int themeIndex;

    public LatentWordByTL(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, int themeIndex, Documents docs) {
        this.docs = docs;
        this.themes = themes;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
        this.themeIndex = themeIndex;
        this.latentWordByTL = new double[docs.nDocuments()][docs.nWords()];
        for (int i = 0; i < docs.nDocuments(); i++) {
            this.latentWordByTL[i] = VariableList.generateRandomDistribution(docs.nWords());
        }
    }

    public static VariableList generateEmptyTopicDist(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs, Documents docs) {
        int nTopics = themes.length();
        Variable[] variables = new LatentWordByTL[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new LatentWordByTL(themes, topicDistDocs, topicDistTLs, i, docs);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int d = 0; d < docs.nDocuments(); d++) {
            for (int w = 0; w < docs.nWords(); w++) {
                double numerator = PSTA.LambdaTL * topicDistTLs
                        .get(themeIndex)
                        .get(docs.get(d).getTimestampId(), docs.get(d).getLocationId());
                double denominator = (1 - PSTA.LambdaTL) * topicDistDocs.get(themeIndex).get(d) + numerator;
                double oldVal = latentWordByTL[d][w];
                double newVal = numerator / denominator;
                converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
                latentWordByTL[d][w] = newVal;
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
        int docIndex = values[0];
        int wordIndex = values[1];
        return latentWordByTL[docIndex][wordIndex];
    }
}