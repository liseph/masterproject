package edu.ntnu.app.lpta;

import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.IntStream;

public class Topics {

    private static double[][] topics; // Periodic topics, bursty topics and then background topic.
    private static boolean hasConverged = false;
    private static int nTopics;

    public static void initialize(int nPeriodicTops) {
        nTopics = nPeriodicTops + 1;
        topics = new double[nTopics][LptaDocs.nWords()];
        // double[] zs = IntStream.range(0, LptaDocs.nWords()).mapToDouble(i -> 1.0 / LptaDocs.nWords()).toArray();
        IntStream.range(0, nTopics).forEach(z -> {
            double[] zss = new Random().doubles(LptaDocs.nWords(), 0, 1).toArray();
            double sum = Arrays.stream(zss).sum();
            double[] zs = Arrays.stream(zss).map(t -> t / sum).toArray();
            topics[z] = zs;
        });
    }

    public static void update() {
        hasConverged = true;
        IntStream.range(0, nTopics).forEach(z -> {
            double[] numerator = IntStream.range(0, LptaDocs.nWords()).mapToDouble(w -> calcAllDocs(w, z)).toArray();
            double denominator = Arrays.stream(numerator).sum();
            double[] newVals = Arrays.stream(numerator).map(val -> val / denominator).toArray();
            hasConverged = hasConverged && IntStream
                    .range(0, LptaDocs.nWords())
                    .allMatch(w -> Math.abs(newVals[w] - topics[z][w]) < Lpta.CONVERGES_LIM);
            topics[z] = newVals;
        });
    }

    private static double calcAllDocs(int w, int z) {
        double v = LptaDocs.getDocsWithWord(w).mapToDouble(d -> calc(d, w, z)).sum();
        return v;
    }

    private static double calc(int d, int w, int z) {
        double v = LptaDocs.getWordCount(d, w) * LatentWordByTopics.get(d, w, z);
        return v;
    }

    public static boolean hasConverged() {
        return hasConverged;
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
        IntStream.range(0, LptaDocs.nWords()).forEach(w -> {
            topTerms.add(new TopicTerm(w, topic[w]));
            while (topTerms.size() > 10) topTerms.poll();
        });
        topTerms.forEach(tt -> tt.setTerm(LptaDocs.getWord(tt.getTermIndex())));
        Object[] objects = topTerms.toArray();
        Arrays.sort(objects, Collections.reverseOrder());
        return Arrays.toString(objects);
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
