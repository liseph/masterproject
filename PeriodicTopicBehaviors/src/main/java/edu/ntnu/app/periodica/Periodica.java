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

    public static final int nTOPICS = 2;
    public static final double TOPIC_PRESENCE_LIM = 2E-1;
    public static final double SMOOTH_PAR = 1E-1;
    public static final double REPERRORLIMIT = 2E-1;

    public static ReferenceSpot[] referenceSpots;

    static public PeriodicaResult[] execute() throws IOException {
        referenceSpots = ReferenceSpot.findReferenceSpots();
        PeriodicaDocs.divideDocsByTimestampAndReferenceSpot(referenceSpots); // For topic analysis
        Topics.analyzeTopics();
        Map<Integer, Map<Double, List<Integer>>> topicPeriodMap = new HashMap<>();
        for (int z = 0; z < nTOPICS; z++) {
            topicPeriodMap.put(z, new HashMap<>());
            for (int o = 1; o < referenceSpots.length; o++) {
                double[] topicPresence = Topics.getTopicPresence(z, o);
                double[] periods = FindPeriodsInTimeseries.execute(new Timeseries(topicPresence, 1));
                for (double p : periods) {
                    topicPeriodMap.get(z).putIfAbsent(p, new ArrayList<>());
                    topicPeriodMap.get(z).get(p).add(o);
                }
            }
        }
        List<PeriodicaResult> results = new ArrayList<>();
        topicPeriodMap.forEach((topicId, periodMap) ->
                periodMap.forEach((period, referenceSpotList) -> {
                    int[][] symbolizedSequence = Topics.getSymbolizedSequence(topicId, referenceSpotList);
                    List<SegmentCluster> result = minePeriodicBehaviours(symbolizedSequence, period);
                    results.add(new PeriodicaResult(result, period, topicId));
                }));
        return results.toArray(PeriodicaResult[]::new);
    }

    private static List<SegmentCluster> minePeriodicBehaviours(int[][] symbolizedSequence, double period) {
        // Segment and init clusters
        int nSegments = (int) (symbolizedSequence.length / period);
        List<SegmentCluster> segments = new ArrayList<>();
        for (int i = 0; i < nSegments; i++) {
            SegmentCluster s = new SegmentCluster(i, period, symbolizedSequence, nSegments);
            segments.add(s);
        }
        // Calculate difference between all clusters
        int initialCap = (nSegments * nSegments - nSegments) / 2;
        List<Double> diffs = new ArrayList<>(initialCap);
        while (diffs.size() < initialCap) diffs.add(0.0);
        double minDiff = Double.POSITIVE_INFINITY;
        int cS = 0;
        int cT = 1;
        for (int i = 0; i < nSegments; i++) {
            for (int j = i + 1; j < nSegments; j++) {
                double diff = calcDiff(segments.get(i), segments.get(j));
                diffs.set(getIndex(i, j, nSegments), diff);
                if (diff < minDiff) {
                    minDiff = diff;
                    cS = i;
                    cT = j;
                }
            }
        }
        // Merge clusters with the smallest difference until representation error makes a sudden jump
        double repError = segments.stream().mapToDouble(s -> s.getRepError()).sum() / nSegments;
        double newRepError = repError;
        while (Math.abs(repError - newRepError) < Periodica.REPERRORLIMIT) {
            segments.get(cS).merge(segments.get(cT));
            segments.remove(cT);
            if (segments.size() == 1) break;

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
                double diff = calcDiff(segments.get(cS), segments.get(j));
                diffs.set(getIndex(cS, j, nSegments), diff);
            }

            // TODO: Consider if I can do this more efficiently, e.g. using a PriorityQueue.
            // Fetch next clusters to merge
            minDiff = Double.POSITIVE_INFINITY;
            for (int i = 0; i < nSegments; i++) {
                for (int j = i + 1; j < nSegments; j++) {
                    double diff = diffs.get(getIndex(i, j, nSegments));
                    if (diff < minDiff) {
                        minDiff = diff;
                        cS = i;
                        cT = j;
                    }
                }
            }

            // Calculate the new representation error if we merge cS and cT in the next iteration.
            repError = newRepError;
            double repErrorMerged = segments.get(cS).calculateNewRepresentationError(segments.get(cT));
            int finalCS = cS;
            int finalCT = cT;
            double repErrorRest = IntStream
                    .range(0, segments.size())
                    .filter(id -> id != finalCS && id != finalCT)
                    .mapToDouble(id -> segments.get(id).getRepError())
                    .sum();
            newRepError = (repErrorMerged + repErrorRest) / nSegments;
        }
        return segments;
    }

    // Returns the Kullback-Leibler divergence between two segments
    private static double calcDiff(SegmentCluster s1, SegmentCluster s2) {
        double result = 0;
        double[][] distMatrix1 = s1.getDistMatrix();
        double[][] distMatrix2 = s2.getDistMatrix();
        for (int t = 0; t < s1.getPeriod(); t++) {
            for (int o = 0; o < PeriodicaDocs.nRefSpots(); o++) {
                result += smooth(distMatrix1[t][o]) * Math.log(smooth(distMatrix1[t][o]) / smooth(distMatrix2[t][o]));
            }
        }
        return result;
    }

    private static double smooth(double d) {
        return (1 - SMOOTH_PAR) * d + SMOOTH_PAR * 1 / PeriodicaDocs.nRefSpots();
    }

    // Assumes row != col
    private static int getIndex(int row, int col, int N) {
        if (row < col)
            return row * (N - 1) - (row - 1) * ((row - 1) + 1) / 2 + col - row - 1;
        else if (col < row)
            return col * (N - 1) - (col - 1) * ((col - 1) + 1) / 2 + row - col - 1;
        throw new IllegalArgumentException("row cannot be equal to col in getIndex function");
    }
}