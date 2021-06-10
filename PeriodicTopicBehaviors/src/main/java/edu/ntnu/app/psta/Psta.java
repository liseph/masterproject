package edu.ntnu.app.psta;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.util.*;

public class Psta {

    // Constants
    public static final double LAMBDA_B = 0.9; // Empirically, a suitable λB for blog documents can be chosen between 0.9 and 0.95.
    public static final double LAMBDA_TL = 0.5; // Empirically, a suitable λTL for blog documents can be chosen between 0.5 and 0.7.
    public static final double CONVERGES_LIM = 1E-2;
    public static Random seedGenerator;
    private static int nTopics;

    static public void clearAll() {
        seedGenerator = null;
        LatentWordByTopic.clear();
        LatentWordByTL.clear();
        Theme.clear();
        TopicDistDoc.clear();
        TopicDistTL.clear();
    }

    static public boolean execute(int nTopics, long seed) {
        seedGenerator = new Random(seed);
        return execute_(nTopics);
    }

    static public boolean execute(int nTopics) {
        seedGenerator = new Random();
        return execute_(nTopics);
    }

    static private boolean execute_(int nTopics_) {
        nTopics = nTopics_;
        // Unknown values
        Theme.initialize(nTopics);
        TopicDistDoc.initialize(nTopics);
        TopicDistTL.initialize(nTopics);

        // Latent variables
        LatentWordByTopic.initialize(nTopics);
        LatentWordByTL.initialize(nTopics);

        boolean converged = false;
        System.out.println("START");
        for (int i = 0; i < 2500; i++) {
            long startTime = System.nanoTime();

            // E-step
            LatentWordByTopic.update();
            boolean b1 = LatentWordByTopic.hasConverged();
            long t1 = (System.nanoTime() - startTime) / 1000000;

            LatentWordByTL.update();
            boolean b2 = LatentWordByTL.hasConverged();
            long t2 = (System.nanoTime() - startTime) / 1000000 - t1;

            // M-step
            Theme.update();
            boolean b3 = Theme.hasConverged();
            long t3 = (System.nanoTime() - startTime) / 1000000 - t2;

            TopicDistDoc.update();
            boolean b4 = TopicDistDoc.hasConverged();
            long t4 = (System.nanoTime() - startTime) / 1000000 - t3;

            TopicDistTL.update();
            boolean b5 = TopicDistTL.hasConverged();
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
        return converged;
    }

    // NOTE: The time series periodicity detection algorithm expects a regularly sampled time series.
    static public PstaPattern[] analyze() {
        Map<Integer, Map<Double, PstaPattern>> patterns = new HashMap<>();
        for (int l = 0; l < PstaDocs.nLocations(); l++) {
            for (int z = 0; z < nTopics; z++) {
                double[] themeLifeCycle = new double[PstaDocs.nTimeslots()];
                double denominator = 0;
                for (int t = 0; t < PstaDocs.nTimeslots(); t++) {
                    themeLifeCycle[t] = calc(t, l, z);
                    denominator += themeLifeCycle[t];
                }
                for (int t = 0; t < PstaDocs.nTimeslots(); t++) {
                    themeLifeCycle[t] = themeLifeCycle[t] / denominator;
                }
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
            }
        }
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

    static private double calc(int t, int l, int z) {
        return TopicDistTL.get(l, t, z) * PstaDocs.getSumWordCount(t, l) / PstaDocs.getSumWordCount();
    }
}
