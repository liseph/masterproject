package edu.ntnu.app.psta;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.util.*;
import java.util.stream.IntStream;

public class Psta {

    // Constants
    public static final double LAMBDA_B = 0.9; // Empirically, a suitable λB for blog documents can be chosen between 0.9 and 0.95.
    public static final double LAMBDA_TL = 0.5; // Empirically, a suitable λTL for blog documents can be chosen between 0.5 and 0.7.
    public static final double CONVERGES_LIM = 1E-2;
    public static final Random seedGenerator = new Random(1000);

    static VariableList themes;
    static VariableList topicDistDocs;
    static VariableList topicDistTLs;

    static public PstaResult execute(int nTopics) {
        // Unknown values
        themes = Theme.generateEmptyThemes(nTopics);
        topicDistDocs = TopicDistDoc.generateEmptyTopicDist(nTopics);
        topicDistTLs = TopicDistTL.generateEmptyTopicDist(nTopics);

        // Latent variables
        VariableList latentWordByTopic = LatentWordByTopic.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs);
        VariableList latentWordByTL = LatentWordByTL.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs);

        // Connect the unknown variables and the latent variables to they can use each other to update themselves
        themes.setVars(latentWordByTopic, latentWordByTL);
        topicDistDocs.setVars(latentWordByTopic, latentWordByTL);
        topicDistTLs.setVars(latentWordByTopic, latentWordByTL);

        boolean converged = false;
        //while (!converged) {
        System.out.println("START");
        for (int i = 0; i < 200; i++) {
            System.out.format("Round %d: ", i + 1);
            long startTime = System.nanoTime();
            // E-step
            latentWordByTopic.updateAll();
            boolean b1 = latentWordByTopic.hasConverged();
            long t1 = System.nanoTime();
            System.out.format("[ %d:%b, ", (t1 - startTime) / 1000000, b1);

            latentWordByTL.updateAll();
            boolean b2 = latentWordByTL.hasConverged();
            long t2 = System.nanoTime();
            System.out.format("%d:%b, ", (t2 - t1) / 1000000, b2);

            // M-step
            themes.updateAll();
            boolean b3 = themes.hasConverged();
            long t3 = System.nanoTime();
            System.out.format("%d:%b, ", (t3 - t2) / 1000000, b3);

            topicDistDocs.updateAll();
            boolean b4 = topicDistDocs.hasConverged();
            long t4 = System.nanoTime();
            System.out.format("%d:%b, ", (t4 - t3) / 1000000, b4);

            topicDistTLs.updateAll();
            boolean b5 = topicDistTLs.hasConverged();
            long t5 = System.nanoTime();
            System.out.format("%d:%b ]\n", (t5 - t4) / 1000000, b5);

            // Check for convergence
            converged = b1 &&
                    b2 &&
                    b3 &&
                    b4 &&
                    b5;
            if (converged) break;
        }
        System.out.println(converged);

        return new PstaResult(themes, topicDistDocs, topicDistTLs);
    }

    // NOTE: The time series periodicity detection algorithm expects a regularly sampled time series.
    static public PstaPattern[] analyze(PstaResult pattern) {
        Map<Double, Map<Integer, PstaPattern>> patterns = new HashMap<>();
        for (int l = 0; l < PstaDocs.nLocations(); l++) {
            TopicDistTL topicDistTL = (TopicDistTL) pattern.getTopicDistTLs().get(l);
            for (int z = 0; z < pattern.nTopics(); z++) {
                int finalZ = z;
                int finalL = l;
                double[] themeLifeCycle = IntStream
                        .range(0, PstaDocs.nTimeslots())
                        .mapToDouble(t -> calc(topicDistTL, t, finalL, finalZ))
                        .toArray();
                double denominator = Arrays.stream(themeLifeCycle).sum();
                themeLifeCycle = Arrays.stream(themeLifeCycle).map(p -> p / denominator).toArray();
                double[] periods = FindPeriodsInTimeseries.execute(new Timeseries(themeLifeCycle, 1));
                if (periods.length > 0) {
                    long[] offsets = findOffsetsFromPeaks(themeLifeCycle, periods);
                    for (int pid = 0; pid < periods.length; pid++) {
                        double p = periods[pid];
                        patterns.putIfAbsent(p, new HashMap<>());
                        patterns.get(p).putIfAbsent(z, new PstaPattern(p, z));
                        patterns.get(p).get(z).addLocation(l, offsets[pid]);
                    }
                }
            }
        }
        return patterns.values().stream().flatMap(m -> m.values().stream()).toArray(PstaPattern[]::new);
    }

    private static long[] findOffsetsFromPeaks(double[] themeLifeCycle, double[] periods) {
        int n = themeLifeCycle.length;
        long[] result = new long[periods.length];
        // Find all peaks
        List<Integer> peakIds = new ArrayList<>();
        if (themeLifeCycle[0] > themeLifeCycle[1]) peakIds.add(0);
        for (int i = 1; i < n - 1; i++) {
            if (themeLifeCycle[i - 1] < themeLifeCycle[i] && themeLifeCycle[i] > themeLifeCycle[i + 1]) {
                peakIds.add(i);
                i++; // No need to check next id as its value will be lower than this value
            }
        }
        if (themeLifeCycle[n - 1] > themeLifeCycle[n - 2]) peakIds.add(n - 1);
        // Find offset per period
        for (int pid = 0; pid < periods.length; pid++) {
            for (int i = 0; i < peakIds.size(); i++) {
                int j = i + 1;
                while (j < peakIds.size() && peakIds.get(j) - peakIds.get(i) < periods[pid]) j++;
                if (j < peakIds.size() && peakIds.get(j) - peakIds.get(i) == periods[pid]) {
                    result[pid] = peakIds.get(i);
                    break;
                }
            }
        }
        return result;
    }

    static private double calc(Variable topicDistTL, int t, int l, int z) {
        return topicDistTL.get(t, z) * PstaDocs.getSumWordCount(t, l) / PstaDocs.getSumWordCount();
    }
}
