package edu.ntnu.app.lpta;

public class LptaResult {
    private final Topics topics;
    private final TopicDistDocs topicDistDocs;
    private final TimeDistTopicLocs timeDistTopicLocs;

    public LptaResult(Topics topics, TopicDistDocs topicDistDocs, TimeDistTopicLocs timeDistTopicLocs) {
        this.topics = topics;
        this.topicDistDocs = topicDistDocs;
        this.timeDistTopicLocs = timeDistTopicLocs;
    }

    public int length() {
        return 0;
    }
}
