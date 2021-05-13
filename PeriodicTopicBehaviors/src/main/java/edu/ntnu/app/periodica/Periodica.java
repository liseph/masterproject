package edu.ntnu.app.periodica;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.ntnu.app.periodica.Main.nTOPICS;

public class Periodica {

    public static final double TOPIC_PRESENCE_LIM = 4E-1;
    public static final double SMOOTH_PAR = 1E-1;
    public static final double REP_ERROR_LIM = 1E-1;

    public static ReferenceSpot[] referenceSpots;

    static public PeriodicaResult[] execute() throws IOException {
        System.out.println("Find ref spots...");
        referenceSpots = ReferenceSpot.findReferenceSpots();
        PeriodicaDocs.divideDocsByTimestampAndReferenceSpot(referenceSpots); // For topic analysis
        System.out.println("Analyze topics...");
        Topics.analyzeTopics();
        Map<Integer, Map<Double, List<Integer>>> topicPeriodMap = new HashMap<>();
        System.out.println("Find periods for each ref spot and topic...");
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
        System.out.println("Mine periodic behaviours...");
        List<PeriodicaResult> results = new ArrayList<>();
        topicPeriodMap.forEach((topicId, periodMap) ->
                periodMap.forEach((period, referenceSpotList) -> {
                    int[][] symbolizedSequence = Topics.getSymbolizedSequence(topicId, referenceSpotList);
                    List<SegmentCluster> result = minePeriodicBehaviours(symbolizedSequence, period);
                    // Discard the clusters with less than two segments, as that is probably not periodic...
                    // Also discard the clusters that only have background probabilities...
                    // If this leads to the result having no segments, discard the result.
                    result = result.stream().filter(res -> res.getSegmentIds().size() > 2 && !res.isOnlyBackground()).collect(Collectors.toList());
                    if (!result.isEmpty())
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
        // Calculate difference between subsequent clusters
        List<Double> diffs = new ArrayList<>();
        while (diffs.size() < nSegments - 1) diffs.add(0.0);
        double minDiff = Double.POSITIVE_INFINITY;
        int cS = 0;
        int cT = 1;
        for (int i = 0; i < nSegments - 1; i++) {
            double diff = calcDiff(segments.get(i), segments.get(i + 1));
            diffs.set(i, diff);
            if (diff < minDiff) {
                minDiff = diff;
                cS = i;
                cT = i + 1;
            }
        }

        // Merge clusters with the smallest difference until representation error makes a sudden jump
        double repError = segments.stream().mapToDouble(s -> s.getRepError()).sum() / nSegments;
        double newRepError = repError;
        while (Math.abs(repError - newRepError) < Periodica.REP_ERROR_LIM) {
            segments.get(cS).merge(segments.get(cT));
            segments.remove(cT);
            if (segments.size() == 1) break;

            // Remove diffs between cluster cT and cT+1
            if (cT != nSegments - 1) diffs.remove(cT);
            nSegments--;

            // Update diff between new cluster cS and the other clusters
            if (cS != nSegments - 1) diffs.set(cS, calcDiff(segments.get(cS), segments.get(cS + 1)));
            if (cS != 0) diffs.set(cS - 1, calcDiff(segments.get(cS - 1), segments.get(cS)));

            // Fetch next clusters to merge
            minDiff = Double.POSITIVE_INFINITY;
            for (int i = 0; i < nSegments - 1; i++) {
                double diff = diffs.get(i);
                if (diff < minDiff) {
                    minDiff = diff;
                    cS = i;
                    cT = i + 1;
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
}