package edu.ntnu.app.periodica;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Periodica {

    static public PeriodicaResult[] execute(int nTopics) {
        ReferenceSpot[] referenceSpots = ReferenceSpot.findReferenceSpots();
        Topic[] topics = Topic.analyzeTopics();
        Map<Integer, Map<Float, List<Integer>>> topicPeriodMap = new HashMap<>();
        for (int z = 0; z < topics.length; z++) {
            topicPeriodMap.put(z, new HashMap<>());
            for (int o = 0; o < referenceSpots.length; o++) {
                Float[] topicPresence = Topic.generateTopicPresence(z, o);
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
                PeriodicaResult result = minePeriodicBehaviours(symbolizedSequence, period, nTopics);
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
