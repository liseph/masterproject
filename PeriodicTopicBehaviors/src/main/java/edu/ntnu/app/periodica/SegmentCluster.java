package edu.ntnu.app.periodica;

import java.util.*;
import java.util.stream.IntStream;

public
class SegmentCluster {
    private final List<Integer> segmentIds;
    private final int period;
    private final double[][] distMatrix;
    private final int[][] symbolizedSequence;
    private double repError;

    public SegmentCluster(int id, double period, int[][] symbolizedSequence, int nSegments) {
        this.segmentIds = new ArrayList<>();
        this.segmentIds.add(id);
        this.period = (int) period;
        this.symbolizedSequence = symbolizedSequence;
        this.distMatrix = new double[(int) period][PeriodicaDocs.nRefSpots()];
        for (int t = 0; t < this.period; t++) {
            for (int o : symbolizedSequence[id * this.period + t]) {
                distMatrix[t][o] += 1;
            }
        }
        for (int t = 0; t < this.period; t++) {
            double tot = Arrays.stream(distMatrix[t]).sum();
            if (tot > 1)
                distMatrix[t] = Arrays.stream(distMatrix[t]).map(val -> val / tot).toArray();
        }
        this.repError = calculateRepresentationError();
    }

    // Merge this segment with input segment
    public void merge(SegmentCluster segment) {
        int cs = segmentIds.size();
        int ct = segment.segmentIds.size();
        double sScale = (double) cs / (cs + ct);
        double tScale = (double) ct / (cs + ct);
        for (int i = 0; i < period; i++) {
            int finalI = i;
            distMatrix[i] = IntStream
                    .range(0, PeriodicaDocs.nRefSpots())
                    .mapToDouble(o -> sScale * distMatrix[finalI][o] + tScale * segment.distMatrix[finalI][o])
                    .toArray();
        }
        segmentIds.addAll(segment.segmentIds);
        this.repError = calculateRepresentationError();
    }

    private double calculateRepresentationError() {
        return calculateNewRepresentationError(null);
    }

    public double calculateNewRepresentationError(SegmentCluster segment2) {
        double newRepError = 0;
        double denominator = 0;
        Iterator<Integer> s1 = segmentIds.iterator();
        Iterator<Integer> s2 = segment2 != null ? segment2.segmentIds.iterator() : Collections.emptyIterator();
        while (s1.hasNext() || s2.hasNext()) {
            int id = s1.hasNext() ? s1.next() : s2.next();
            for (int t = 0; t < period; t++) {
                for (int o : symbolizedSequence[id * period + t]) {
                    if (o == 0)
                        continue;
                    denominator++;
                    newRepError += 1 - distMatrix[t][o];
                }
            }
        }
        newRepError /= denominator;
        return denominator != 0 ? newRepError : 0;
    }

    public double getRepError() {
        return repError;
    }

    public List<Integer> getSegmentIds() {
        return segmentIds;
    }

    public double[][] getDistMatrix() {
        return distMatrix;
    }

    public int getPeriod() {
        return period;
    }

    public String getLocationTrajectory() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int t = 0; t < distMatrix.length; t++) {
            if (distMatrix[t][0] == 1.0) continue;
            builder.append("t");
            builder.append(t);
            builder.append(":");
            for (int o = 1; o < distMatrix[t].length; o++) {
                if (distMatrix[t][o] > 0) {
                    builder.append(String.format("%d(%.2f)", o, distMatrix[t][o]));
                }
            }
            builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }

    public boolean isOnlyBackground() {
        return Arrays.stream(distMatrix).allMatch(topics -> topics[0] == 1.0);
    }
}
