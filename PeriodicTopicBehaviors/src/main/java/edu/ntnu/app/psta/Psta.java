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
    public static Random seedGenerator;

    static VariableList themes;
    static VariableList topicDistDocs;
    static VariableList topicDistTLs;

    static public void clearAll() {
        seedGenerator = null;
        themes = null;
        topicDistDocs = null;
        topicDistTLs = null;
    }

    static public PstaResult execute(int nTopics) {
        return execute(nTopics, 1000);
    }

    static public PstaResult execute(int nTopics, long seed) {
        seedGenerator = new Random(seed);
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
        System.out.println("START");
        for (int i = 0; i < 200; i++) {
            long startTime = System.nanoTime();

            // E-step
            latentWordByTopic.updateAll();
            boolean b1 = latentWordByTopic.hasConverged();
            long t1 = (System.nanoTime() - startTime) / 1000000;

            latentWordByTL.updateAll();
            boolean b2 = latentWordByTL.hasConverged();
            long t2 = (System.nanoTime() - startTime) / 1000000 - t1;

            // M-step
            themes.updateAll();
            boolean b3 = themes.hasConverged();
            long t3 = (System.nanoTime() - startTime) / 1000000 - t2;

            topicDistDocs.updateAll();
            boolean b4 = topicDistDocs.hasConverged();
            long t4 = (System.nanoTime() - startTime) / 1000000 - t3;

            topicDistTLs.updateAll();
            boolean b5 = topicDistTLs.hasConverged();
            long t5 = (System.nanoTime() - startTime) / 1000000 - t4;

            if (i % 10 == 0) {
                System.out.format("Round %d: [ %d:%b, %d:%b, %d:%b, %d:%b, %d:%b ]\n",
                        i + 1, t1, b1, t2, b2, t3, b3, t4, b4, t5, b5);
            }

            // Check for convergence
            converged = b1 && b2 && b3 && b4 && b5;
            if (converged) {
                System.out.format("Break on iteration %d.\n", i + 1);
                break;
            }
        }
        System.out.println(converged);

        return new PstaResult(themes, topicDistDocs, topicDistTLs);
    }

    // NOTE: The time series periodicity detection algorithm expects a regularly sampled time series.
    static public PstaPattern[] analyze(PstaResult pattern) {
        Map<Integer, Map<Double, PstaPattern>> patterns = new HashMap<>();
        IntStream.range(0, PstaDocs.nLocations()).forEach(l -> {
            TopicDistTL topicDistTL = (TopicDistTL) pattern.getTopicDistTLs().get(l);
            IntStream.range(0, pattern.nTopics()).forEach(z -> {
                double[] themeLifeCycle = IntStream
                        .range(0, PstaDocs.nTimeslots())
                        .mapToDouble(t -> calc(topicDistTL, t, l, z))
                        .toArray();
                double denominator = Arrays.stream(themeLifeCycle).sum();
                themeLifeCycle = Arrays.stream(themeLifeCycle).map(p -> p / denominator).toArray();
                double[] periods = FindPeriodsInTimeseries.execute(new Timeseries(themeLifeCycle, 1));
                if (periods.length > 0) {
                    double[] offsets = findOffsetsFromPeaks(themeLifeCycle, periods);
                    for (int pid = 0; pid < periods.length; pid++) {
                        if (offsets[pid] == -1) continue;
                        double p = periods[pid];
                        patterns.putIfAbsent(z, new HashMap<>());
                        patterns.get(z).putIfAbsent(p, new PstaPattern(p, z));
                        patterns.get(z).get(p).addLocation(l, offsets[pid]);
                    }
                }
            });
        });
        return patterns.values().stream().flatMap(m -> m.values().stream()).toArray(PstaPattern[]::new);
    }

    // Function to find offsets of the patterns by identifying the peaks of the themelifecycle and finding which peaks
    // belong to the pattern, and then selecting the first of these peaks. If no peaks are found to fit the pattern, the
    // pattern is discarded.
    private static double[] findOffsetsFromPeaks(double[] themeLifeCycle, double[] periods) {
        int n = themeLifeCycle.length;
        double[] result = new double[periods.length];
        Arrays.fill(result, -1);
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
        int absOffsetLimit = themeLifeCycle.length / 3;
        for (int pid = 0; pid < periods.length; pid++) {
            for (int i = 0; i < peakIds.size(); i++) {
                if (peakIds.get(i) > absOffsetLimit)
                    break;
                int j = i + 1;
                while (j < peakIds.size() && peakIds.get(j) - peakIds.get(i) < periods[pid]) j++;
                // If the difference between the two peaks are the period, add the first peak as offset
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
