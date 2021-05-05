package edu.ntnu.app.periodica;

import java.util.Arrays;
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
                ", topic=" + topic + Topics.getTopicString(topic) +
                ", #clusters=" + segments.size() +
                ", movement=" + Arrays.toString(
                segments.stream().map(SegmentCluster::getLocationTrajectory).toArray()) +
                ", distMatrices=" + Arrays.toString(
                segments.stream().map(seg -> Arrays.toString(Arrays.stream(seg.getDistMatrix()).map(Arrays::toString).toArray())).toArray()) +
                ", segments=" + getSegmentIds(segments) +
                "}";
    }

    private String getSegmentIds(List<SegmentCluster> segments) {
        return segments.stream().map(s -> getStringRep(s.getSegmentIds())).collect(Collectors.joining(", ", "[", "]"));
    }

    private String getStringRep(List<Integer> ids) {
        return ids.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
