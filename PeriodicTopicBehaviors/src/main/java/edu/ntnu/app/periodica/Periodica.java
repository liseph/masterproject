package edu.ntnu.app.periodica;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Periodica {

    public static final int nTOPICS = 20;

    static public PeriodicaResult[] execute() throws IOException {
        ReferenceSpot[] referenceSpots = ReferenceSpot.findReferenceSpots();
        PeriodicaDocs.divideDocsByTimestampAndReferenceSpot(referenceSpots); // For topic analysis
        Topics topics = Topics.analyzeTopics();
        Map<Integer, Map<Float, List<Integer>>> topicPeriodMap = new HashMap<>();
        for (int z = 0; z < nTOPICS; z++) {
            topicPeriodMap.put(z, new HashMap<>());
            for (int o = 0; o < referenceSpots.length; o++) {
                Float[] topicPresence = Topics.generateTopicPresence(z, o);
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
                int[][] symbolizedSequence = getSymbolizedSequence(referenceSpotList);
                PeriodicaResult result = minePeriodicBehaviours(symbolizedSequence, period, nTOPICS);
                results.add(result);
            });
        });
        return results.toArray(PeriodicaResult[]::new);
    }

    private static PeriodicaResult minePeriodicBehaviours(int[][] symbolizedSequence, Float period, int nTopics) {
        return null;
    }

    private static int[][] getSymbolizedSequence(List<Integer> referenceSpotList) {
        return new int[0][];
    }

}
