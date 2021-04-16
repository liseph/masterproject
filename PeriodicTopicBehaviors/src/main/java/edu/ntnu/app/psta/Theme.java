package edu.ntnu.app.psta;

import java.util.Arrays;
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
        this.wordDistribution = VariableList.generateRandomDistribution(Docs.nWords());
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
        double denominator = IntStream.range(0, Docs.nWords()).mapToDouble(w2 -> baseCalcForAllDocs(w2)).sum();
        for (int w = 0; w < Docs.nWords(); w++) {
            double numerator = baseCalcForAllDocs(w);
            double oldVal = wordDistribution[w];
            double newVal = denominator != 0 ? numerator / denominator : 0;
            converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
            wordDistribution[w] = newVal;
        }
        return converges;
    }

    private double baseCalc(int d, int w) {
        return Docs.getWordCount(d, w) * latentWordByTopic.get(d).get(w, id);
    }

    private double baseCalcForAllDocs(int w) {
        return Docs.getIndexOfDocsWithWord(w).mapToDouble(d -> baseCalc(d, w)).sum();
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

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        Map<Double, String> wordDistributionMap = new TreeMap<>();
        String[] vocabulary = Docs.getVocabulary();
        for (int i = 0; i < Docs.nWords(); i++) {
            wordDistributionMap.put(wordDistribution[i], vocabulary[i]);
        }
        return "\np(w|z){" +
                "z=" + id +
                "[word:prob]=" + mapToString(wordDistributionMap) +
                '}';
    }

    private String mapToString(Map<Double, String> map) {
        String mapAsString = map.entrySet().stream()
                .map(entry -> entry.getValue() + ":" + entry.getKey())
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }
}
