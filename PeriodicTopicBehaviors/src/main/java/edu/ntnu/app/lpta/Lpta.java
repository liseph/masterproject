package edu.ntnu.app.lpta;

import java.util.ArrayList;
import java.util.List;

public class Lpta {

    public static final double CONVERGES_LIM = 1E-2;

    public static void execute(int nPeriodicTopics, double[] periods) {
        // Latent variable
        LatentWordByTopics.initialize(nPeriodicTopics);

        // Unknown variables
        Topics.initialize(nPeriodicTopics);
        TopicDistDocs.initialize(nPeriodicTopics);
        TimeDistTopicLocs.initialize(nPeriodicTopics, periods);

        boolean converged = false;
        System.out.println("START EM ALGORITHM");
        for (int i = 0; i < 300; i++) {
            long startTime = System.nanoTime();

            // E-step
            LatentWordByTopics.update();
            boolean b1 = LatentWordByTopics.hasConverged();
            long t1 = (System.nanoTime() - startTime) / 1000000;

            // M-step
            Topics.update();
            boolean b2 = Topics.hasConverged();
            long t2 = (System.nanoTime() - startTime) / 1000000 - t1;

            TopicDistDocs.update();
            boolean b3 = TopicDistDocs.hasConverged();
            long t3 = (System.nanoTime() - startTime) / 1000000 - t2;

            TimeDistTopicLocs.update();
            boolean b4 = TimeDistTopicLocs.hasConverged();
            long t4 = (System.nanoTime() - startTime) / 1000000 - t3;

            if (i % 10 == 0) {
                System.out.format("Round %d: [ %d:%b, %d:%b, %d:%b, %d:%b ]\n",
                        i + 1, t1, b1, t2, b2, t3, b3, t4, b4);
            }

            // Check for convergence
            converged = b1 && b2 && b3 && b4;
            if (converged) {
                System.out.format("Break on iteration %d.\n", i + 1);
                break;
            }
        }
    }

    public static List<LptaPattern> analyze(int nTopics, double[] periods) {
        List<LptaPattern> patterns = new ArrayList<>();
        for (int z = 0; z < nTopics; z++) {
            LptaPattern pattern = new LptaPattern(periods[z], z);
            for (int l = 0; l < LptaDocs.nLocations(); l++) {
                double stdDev = TimeDistTopicLocs.getStdDeviation(l, z);
                double mean = TimeDistTopicLocs.getMean(l, z);
                if (mean != 0 && stdDev == 0) {
                    System.out.println("hei");
                }
                if (mean == 0 && stdDev != 0) {
                    System.out.println("heihop");
                }
                if (mean != 0 || stdDev >= TimeDistTopicLocs.STD_DEVIATION_MIN)
                    pattern.addLocation(l, TimeDistTopicLocs.getMean(l, z), stdDev);

            }
            if (pattern.getLocationTrajectory().length == 0) continue;
            pattern.setTimeDist(TimeDistTopicLocs.getTimeDist(z, pattern.getLocationTrajectory()));
            pattern.setOffset();
            patterns.add(pattern);
        }
        return patterns;
    }
}
