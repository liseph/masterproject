package edu.ntnu.app.lpta;

import java.util.ArrayList;
import java.util.List;

public class Lpta {

    public static final double CONVERGES_LIM = 1E-1;

    public static void execute(int nPeriodicTopics, double[] periods) {
        // Latent variable
        LatentWordByTopics.initialize(nPeriodicTopics);

        // Unknown variables, initialization assumes LatentWordByTopics have already been initialized
        Topics.initialize(nPeriodicTopics);
        TopicDistDocs.initialize(nPeriodicTopics);
        TimeDistTopicLocs.initialize(nPeriodicTopics, periods);

        boolean converged = false;
        System.out.println("START EM ALGORITHM");
        for (int i = 0; i < 500; i++) {
            System.out.format("Round %d: ", i + 1);
            long startTime = System.nanoTime();

            // E-step
            LatentWordByTopics.update();
            boolean b1 = LatentWordByTopics.hasConverged();
            long t1 = System.nanoTime();
            System.out.format("[ %d:%b, ", (t1 - startTime) / 1000000, b1);

            // M-step
            Topics.update();
            boolean b2 = Topics.hasConverged();
            long t2 = System.nanoTime();
            System.out.format("%d:%b, ", (t2 - t1) / 1000000, b2);

            TopicDistDocs.update();
            boolean b3 = TopicDistDocs.hasConverged();
            long t3 = System.nanoTime();
            System.out.format("%d:%b, ", (t3 - t2) / 1000000, b3);

            TimeDistTopicLocs.update();
            boolean b4 = TimeDistTopicLocs.hasConverged();
            long t4 = System.nanoTime();
            System.out.format("%d:%b ]\n", (t4 - t3) / 1000000, b4);

            converged = b1 && b2 && b3 && b4;
            if (converged) break;
        }
    }

    public static List<LptaPattern> analyze(int nTopics, double[] periods) {
        List<LptaPattern> patterns = new ArrayList<>();
        for (int z = 0; z < nTopics; z++) {
            LptaPattern pattern = new LptaPattern(periods[z], z);
            for (int l = 0; l < LptaDocs.nLocations(); l++) {
                if (TimeDistTopicLocs.getStdDeviation(l, z) >= TimeDistTopicLocs.STD_DEVIATION_MIN) {
                    pattern.addLocation(l, TimeDistTopicLocs.getMean(l, z));
                }
            }
            if (pattern.getLocationTrajectory().length == 0) continue;
            pattern.setTimeDist(TimeDistTopicLocs.getTimeDist(z, pattern.getLocationTrajectory()));
            pattern.setOffset();
            patterns.add(pattern);
        }
        return patterns;
    }
}
