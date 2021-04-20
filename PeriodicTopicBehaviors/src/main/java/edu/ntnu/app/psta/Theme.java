package edu.ntnu.app.psta;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Theme implements Variable {

    private static int idCount = 0;

    private final Float[] wordDistribution;
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
        Float denominator = IntStream.range(0, Docs.nWords()).mapToObj(w2 -> baseCalcForAllDocs(w2)).reduce(0f, Float::sum);
        for (int w = 0; w < Docs.nWords(); w++) {
            Float numerator = baseCalcForAllDocs(w);
            Float oldVal = wordDistribution[w];
            Float newVal = denominator != 0 ? numerator / denominator : 0;
            converges = converges && Math.abs(oldVal - newVal) < PSTA.EPSILON;
            wordDistribution[w] = newVal;
        }
        return converges;
    }

    private Float baseCalc(int d, int w) {
        return Docs.getWordCount(d, w) * latentWordByTopic.get(d).get(w, id);
    }

    private Float baseCalcForAllDocs(int w) {
        return Docs.getIndexOfDocsWithWord(w).mapToObj(d -> baseCalc(d, w)).reduce(0f, Float::sum);
    }

    public void setVars(VariableList latentWordByTopic, VariableList ignoreMe) {
        this.latentWordByTopic = latentWordByTopic;
    }

    @Override
    public Float get(int... wordIndex) {
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
        Map<Float, String> wordDistributionMap = new TreeMap<>(Collections.reverseOrder());
        String[] vocabulary = Docs.getVocabulary();
        for (int i = 0; i < Docs.nWords(); i++) {
            wordDistributionMap.put(wordDistribution[i], vocabulary[i]);
        }
        return "\np(w|z){" +
                "z=" + id +
                "[word:prob]=" + mapToString(wordDistributionMap) +
                '}';
    }

    private String mapToString(Map<Float, String> map) {
        String mapAsString = map.entrySet().stream().limit(20)
                .map(entry -> entry.getValue() + ":" + entry.getKey())
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }
}
