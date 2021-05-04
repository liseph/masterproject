package edu.ntnu.app.psta;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Theme implements Variable {

    private static int idCount = 0;


    private final double[] wordDistribution;
    private final int id;
    private VariableList latentWordByTopic;

    public Theme() {
        this.id = idCount++;
        long SEED = Psta.seedGenerator.nextLong();
        this.wordDistribution = VariableList.generateRandomDistribution(PstaDocs.nWords(), SEED);
    }

    public static VariableList generateEmptyThemes(int nTopics) {
        Variable[] variables = new Theme[nTopics];
        for (int i = 0; i < nTopics; i++) {
            variables[i] = new Theme();
        }
        return new VariableList(variables);
    }

    @Override
    public boolean update() {
        boolean converges = true;
        double denominator = IntStream.range(0, PstaDocs.nWords()).mapToDouble(this::baseCalcForAllDocs).sum();
        for (int w = 0; w < PstaDocs.nWords(); w++) {
            double numerator = baseCalcForAllDocs(w);
            double oldVal = wordDistribution[w];
            double newVal = denominator != 0 ? numerator / denominator : 0;
            converges = converges && Math.abs(oldVal - newVal) < Psta.EPSILON;
            wordDistribution[w] = newVal;
        }
        return converges;
    }

    private double baseCalc(int d, int w) {
        return latentWordByTopic.get(d).get(w, id) * PstaDocs.getWordCount(d, w);
    }

    private double baseCalcForAllDocs(int w) {
        return PstaDocs.getIndexOfDocsWithWord(w).mapToDouble(d -> baseCalc(d, w)).sum();
    }

    public void setVars(VariableList latentWordByTopic, VariableList ignoreMe) {
        this.latentWordByTopic = latentWordByTopic;
    }

    @Override
    public double get(int... wordIndex) {
        if (wordIndex.length != 1) {
            throw new IllegalArgumentException("Wrong number of values passed to Theme.get(). It should be 1.");
        }
        return wordDistribution[wordIndex[0]];
    }

    @Override
    public String toString() {
        Map<Double, String> wordDistributionMap = new TreeMap<>(Collections.reverseOrder());
        for (int i = 0; i < PstaDocs.nWords(); i++) {
            wordDistributionMap.put(wordDistribution[i], PstaDocs.getWord(i));
        }
        return "p(w|z){" +
                "z=" + id +
                ", [word:prob]=" + mapToString(wordDistributionMap) +
                '}';
    }

    private String mapToString(Map<Double, String> map) {
        return map.entrySet().stream().limit(20)
                .map(entry -> entry.getValue() + ":" + entry.getKey())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
