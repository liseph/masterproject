package edu.ntnu.app.periodica;

import java.util.List;
import java.util.stream.Collectors;

public class PeriodicaResult {
    private final List<SegmentCluster> segments;
    private final double period;
    private final Integer topic;

    public PeriodicaResult(List<SegmentCluster> result, Double period, Integer topicId) {
        this.segments = result;
        this.period = period;
        this.topic = topicId;
    }

    @Override
    public String toString() {
        return "PeriodicaResult{" +
                "period=" + period +
                ", segments=" + getSegmentIds(segments) +
                ", topic=id" + topic +
                ", " + Topics.getTopicString(topic) +
                "}";
    }

    private String getSegmentIds(List<SegmentCluster> segments) {
        return segments.stream().map(s -> getStringRep(s.getSegmentIds())).collect(Collectors.joining(", ", "[", "]"));
    }

    private String getStringRep(List<Integer> ids) {
        return ids.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
