package edu.ntnu.app.lpta;

public class LptaResult {
    private final double[][] topics;
    private final double[][] topicDistDocs;
    private final double[][][] timeDistTopicLocs;

    public LptaResult(double[][] topics, double[][] topicDistDocs, double[][][] timeDistTopicLocs) {
        this.topics = topics;
        this.topicDistDocs = topicDistDocs;
        this.timeDistTopicLocs = timeDistTopicLocs;
    }

    public int length() {
        return 0;
    }
}
