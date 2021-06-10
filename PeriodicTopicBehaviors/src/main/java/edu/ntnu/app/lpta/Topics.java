package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

public class Topics {

    private static double[][] topics; // Periodic topics, bursty topics and then background topic.
    private static boolean converges = false;
    private static int nTopics;

    public static void initialize(int nPeriodicTops) {
        nTopics = nPeriodicTops + 1;
        topics = new double[nTopics][];
        double[] zs = new double[LptaDocs.nWords()];
        Arrays.fill(zs, 1.0 / LptaDocs.nWords());
        for (int z = 0; z < nTopics; z++) {
            topics[z] = zs;
        }
    }

    public static void update() {
        converges = true;
        for (int z = 0; z < nTopics; z++) {
            double[] numerator = new double[LptaDocs.nWords()];
            double denominator = 0;
            for (int w = 0; w < LptaDocs.nWords(); w++) {
                numerator[w] = calcAllDocs(w, z);
                denominator += numerator[w];
            }
            for (int w = 0; w < LptaDocs.nWords(); w++) {
                numerator[w] /= denominator;
                converges = converges && Math.abs(numerator[w] - topics[z][w]) < Lpta.CONVERGES_LIM;
            }
            topics[z] = numerator;
        }
    }

    private static double calcAllDocs(int w, int z) {
        return LptaDocs.getDocsWithWord(w).mapToDouble(d -> calc(d, w, z)).sum();
    }

    private static double calc(int d, int w, int z) {
        return LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z);
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static double[][] getDistribution() {
        return topics;
    }

    public static double get(int z, int w) {
        return topics[z][w];
    }

    public static String getTopTermsInTopicAsString(int themeIndex) {
        double[] topic = topics[themeIndex];
        PriorityQueue<TopicTerm> topTerms = new PriorityQueue<>();
        for (int w = 0; w < LptaDocs.nWords(); w++) {
            topTerms.add(new TopicTerm(w, topic[w]));
            while (topTerms.size() > 10) topTerms.poll();
        }
        topTerms.forEach(tt -> tt.setTerm(LptaDocs.getWord(tt.getTermIndex())));
        Object[] objects = topTerms.toArray();
        Arrays.sort(objects, Collections.reverseOrder());
        return Arrays.toString(objects);
    }

    public static void clear() {
        topics = null;
        converges = false;
        nTopics = 0;
    }

    static class TopicTerm implements Comparable<TopicTerm> {
        int termIndex;
        String term;
        double probVal;

        public TopicTerm(int termIndex, double probVal) {
            this.termIndex = termIndex;
            this.probVal = probVal;
        }

        public int getTermIndex() {
            return termIndex;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        @Override
        public int compareTo(TopicTerm topicTerm) {
            return Double.compare(this.probVal, topicTerm.probVal);
        }

        @Override
        public String toString() {
            return String.format("%s: %.4f", term, probVal);
        }
    }
}
