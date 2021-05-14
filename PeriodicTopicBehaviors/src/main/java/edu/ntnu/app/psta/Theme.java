package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Theme {

    private static double[][] wordDistribution;
    private static int nTopics;
    private static boolean converges;

    public static void initialize(int nTopics_) {
        nTopics = nTopics_;
        converges = false;
        wordDistribution = new double[nTopics][PstaDocs.nWords()];
        double[] init = new double[PstaDocs.nWords()];
        Arrays.fill(init, 1.0 / PstaDocs.nWords());
        for (int z = 0; z < nTopics; z++) {
            wordDistribution[z] = init;
        }
    }

    public static void update() {
        converges = true;
        for (int z = 0; z < nTopics; z++) {
            double[] numerator = new double[PstaDocs.nWords()];
            double denominator = 0;
            for (int w = 0; w < PstaDocs.nWords(); w++) {
                numerator[w] = baseCalcForAllDocs(w, z);
                denominator += numerator[w];
            }
            double uniform = 1.0 / PstaDocs.nWords();
            for (int w = 0; w < PstaDocs.nWords(); w++) {
                numerator[w] = denominator != 0 ? numerator[w] / denominator : uniform;
                converges = converges && Math.abs(numerator[w] - wordDistribution[z][w]) < Psta.CONVERGES_LIM;
            }
            wordDistribution[z] = numerator;
        }
    }

    private static double baseCalc(int d, int w, int z) {
        return LatentWordByTopic.get(d, w, z) * PstaDocs.getWordCount(d, w);
    }

    private static double baseCalcForAllDocs(int w, int z) {
        return PstaDocs.getIndexOfDocsWithWord(w).mapToDouble(d -> baseCalc(d, w, z)).sum();
    }

    public static double get(int topicIndex, int wordIndex) {
        return wordDistribution[topicIndex][wordIndex];
    }

    public static void clear() {
        wordDistribution = null;
        nTopics = 0;
        converges = false;
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static String getAsString() {
        StringBuilder builder = new StringBuilder();
        for (int z = 0; z < nTopics; z++) {
            Map<Double, String> wordDistributionMap = new TreeMap<>(Collections.reverseOrder());
            for (int i = 0; i < PstaDocs.nWords(); i++) {
                wordDistributionMap.put(wordDistribution[z][i], PstaDocs.getWord(i));
            }
            builder.append("p(w|z){z=");
            builder.append(z);
            builder.append(", [word:prob]=");
            builder.append(mapToString(wordDistributionMap));
            builder.append("}\n");
        }
        return builder.toString();
    }

    private static String mapToString(Map<Double, String> map) {
        return map.entrySet().stream().limit(10)
                .map(entry -> String.format("%s: %.4f", entry.getValue(), entry.getKey()))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
