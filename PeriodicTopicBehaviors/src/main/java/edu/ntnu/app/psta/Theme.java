package edu.ntnu.app.psta;

import java.util.stream.IntStream;

public class Theme implements Variable {

    private static int idCount = 0;

    private final double[] wordDistribution;
    private final int id;
    private final Documents docs;
    private VariableList latentWordByTopic;
    private VariableList latentWordByTL;

    public Theme(Documents docs) {
        this.id = idCount++;
        this.wordDistribution = VariableList.generateRandomDistribution(docs.nWords());
        this.docs = docs;
    }

    public static VariableList generateEmptyThemes(int nTopics, Documents docs) {
        Variable[] variables = new Theme[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new Theme(docs);
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        for (int w = 0; w < docs.nWords(); w++) {
            double numerator = baseCalcForAllDocs(id, w);
            double denominator = IntStream.range(0, docs.nWords()).mapToDouble(w2 -> baseCalcForAllDocs(id, w2)).sum();
            double oldVal = wordDistribution[w];
            double newVal = numerator / denominator;
            converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
            wordDistribution[w] = newVal;
        }
        return converges;
    }

    private double baseCalc(int z, int d, int w) {
        return docs.getWordCount(d, w) * latentWordByTopic.get(id).get(d, w);
    }

    private double baseCalcForAllDocs(int z, int w) {
        return IntStream.range(0, docs.nDocuments()).mapToDouble(d -> baseCalc(z, d, w)).sum();
    }

    public void setVars(VariableList latentWordByTopic, VariableList latentWordByTL) {
        this.latentWordByTopic = latentWordByTopic;
        this.latentWordByTL = latentWordByTL;
    }

    @Override
    public double get(int... wordIndex) {
        if (wordIndex.length != 1) {
            throw new IllegalArgumentException("Wrong number of values passed to Theme.get(). It should be 1.");
        }
        return wordDistribution[wordIndex[0]];
    }

    public int getId() {
        return id;
    }
}
