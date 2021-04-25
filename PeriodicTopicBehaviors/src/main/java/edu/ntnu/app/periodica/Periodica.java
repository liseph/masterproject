package edu.ntnu.app.periodica;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class Periodica {

    public static final int nTOPICS = 20;
    public static final double EPSILON = 1E-2;
    public static final double LAMBDA = 1E-1;

    static public PeriodicaResult[] execute() throws IOException {
        ReferenceSpot[] referenceSpots = ReferenceSpot.findReferenceSpots();
        PeriodicaDocs.divideDocsByTimestampAndReferenceSpot(referenceSpots); // For topic analysis
        Topics.analyzeTopics();
        Map<Integer, Map<Float, List<Integer>>> topicPeriodMap = new HashMap<>();
        for (int z = 0; z < nTOPICS; z++) {
            topicPeriodMap.put(z, new HashMap<>());
            for (int o = 0; o < referenceSpots.length; o++) {
                Float[] topicPresence = Topics.getTopicPresence(z, o);
                Float[] periods = FindPeriodsInTimeseries.execute(new Timeseries(topicPresence, 1));
                for (Float p : periods) {
                    topicPeriodMap.get(z).putIfAbsent(p, new ArrayList<>());
                    topicPeriodMap.get(z).get(p).add(o);
                }
            }
        }
        List<PeriodicaResult> results = new ArrayList<>();
        topicPeriodMap.forEach((topicId, periodMap) -> {
            periodMap.forEach((period, referenceSpotList) -> {
                int[][] symbolizedSequence = Topics.getSymbolizedSequence(topicId, referenceSpotList);
                List<Segment> result = minePeriodicBehaviours(symbolizedSequence, period);
                results.add(new PeriodicaResult(result));
            });
        });
        return results.toArray(PeriodicaResult[]::new);
    }

    private static List<Segment> minePeriodicBehaviours(int[][] symbolizedSequence, Float period) {
        // Segment and init clusters
        int nSegments = (int) (symbolizedSequence.length / period);
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < nSegments; i++) {
            Segment s = new Segment(i, period, symbolizedSequence);
            segments.add(s);
        }
        // Calculate difference between all clusters
        List<Float> diffs = new ArrayList<>((nSegments * nSegments - nSegments) / 2);
        float minDiff = Float.POSITIVE_INFINITY;
        int cS = 0;
        int cT = 1;
        for (int i = 0; i < nSegments; i++) {
            for (int j = i + 1; j < nSegments; j++) {
                if (i == j) continue;
                float diff = calcDiff(segments.get(i), segments.get(j));
                diffs.set(getIndex(i, j, nSegments), diff);
                if (diff < minDiff) {
                    minDiff = diff;
                    cS = i;
                    cT = j;
                }
            }
        }
        // Merge clusters with the smallest difference
        int K = 5; // TODO: Implement representation error
        while (nSegments > K) {
            segments.get(cS).merge(segments.get(cT));
            segments.remove(cT);
            // Remove diffs for cluster cT. Remove from last to first to not fuck up indexes
            for (int i = nSegments - 1; i >= 0; i--) {
                if (i == cT) continue;
                int index = getIndex(i, cT, nSegments);
                diffs.remove(index);
            }
            nSegments--;

            // Update diff between new cluster, cS, and the other clusters
            for (int j = 0; j < nSegments; j++) {
                if (cS == j) continue;
                float diff = calcDiff(segments.get(cS), segments.get(j));
                diffs.set(getIndex(cS, j, nSegments), diff);
            }

            // TODO: Consider if I can do this more efficiently, e.g. using a PriorityQueue.
            minDiff = Float.POSITIVE_INFINITY;
            for (int i = 0; i < nSegments; i++) {
                for (int j = i + 1; j < nSegments; j++) {
                    if (i == j) continue;
                    float diff = diffs.get(getIndex(i, j, nSegments));
                    if (diff < minDiff) {
                        minDiff = diff;
                        cS = i;
                        cT = j;
                    }
                }
            }
        }
        return segments;
    }

    // Returns the Kullback-Leibler divergence between two segments
    private static float calcDiff(Segment s1, Segment s2) {
        double result = 0;
        double[][] distMatrix1 = s1.getDistMatrix();
        double[][] distMatrix2 = s2.getDistMatrix();
        for (int t = 0; t < s1.getPeriod(); t++) {
            for (int o = 0; o < PeriodicaDocs.nRefSpots(); o++) {
                result += smooth(distMatrix1[t][o]) * Math.log(smooth(distMatrix1[t][o]) / smooth(distMatrix2[t][o]));
            }
        }
        return (float) result;
    }

    private static double smooth(double d) {
        return (1 - LAMBDA) * d + LAMBDA * 1 / PeriodicaDocs.nRefSpots();
    }

    // Assumes row != col
    private static int getIndex(int row, int col, int N) {
        if (row < col)
            return row * (N - 1) - (row - 1) * ((row - 1) + 1) / 2 + col - row - 1;
        return col * (N - 1) - (col - 1) * ((col - 1) + 1) / 2 + row - col - 1;
    }

}

class Segment {
    private final List<Integer> ids;
    private final int period;
    private final double[][] distMatrix;

    public Segment(int id, float period, int[][] symbolizedSequence) {
        this.ids = new ArrayList<>();
        this.ids.add(id);
        this.period = (int) period;
        this.distMatrix = new double[(int) period][PeriodicaDocs.nRefSpots()];
        for (int t = 0; t < this.period; t++) {
            for (int o = 0; o < symbolizedSequence[id * this.period + t].length; t++) {
                distMatrix[t][o] = 1; // Only one segment
                // TODO: It makes sense that it should not sum to 1, but does it still work when it doesn't sum to 1?
            }
        }
    }

    // Merge this segment with input segment
    public void merge(Segment segment) {
        int cs = ids.size();
        int ct = segment.ids.size();
        float sScale = (float) cs / (cs + ct);
        float tScale = (float) ct / (cs + ct);
        for (int i = 0; i < period; i++) {
            int finalI = i;
            distMatrix[i] = IntStream
                    .range(0, PeriodicaDocs.nRefSpots())
                    .mapToDouble(o -> sScale * distMatrix[finalI][o] + tScale * segment.distMatrix[finalI][o])
                    .toArray();
        }
        ids.addAll(segment.ids);
    }

    public List<Integer> getIds() {
        return ids;
    }

    public double[][] getDistMatrix() {
        return distMatrix;
    }

    public int getPeriod() {
        return period;
    }
}
