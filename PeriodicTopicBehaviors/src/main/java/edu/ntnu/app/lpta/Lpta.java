package edu.ntnu.app.lpta;

public class Lpta {

    public static final double EPSILON = 1E-2f;

    public static LptaResult execute(int nPeriodicTopics, int nBurstyTopics, double[] periods) {
        // Latent variable
        LatentWordByTopics.initialize(nPeriodicTopics, nBurstyTopics);

        // Unknown variables, initialization assumes LatentWordByTopics have already been initialized
        Topics.initialize(nPeriodicTopics, nBurstyTopics);
        TopicDistDocs.initialize(nPeriodicTopics, nBurstyTopics);
        TimeDistTopicLocs.initialize(nPeriodicTopics, nBurstyTopics, periods);

        boolean converged = false;
        System.out.println("START EM ALGORITHM");
        for (int i = 0; i < 100; i++) {
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

        return new LptaResult(Topics.getDistribution(), TopicDistDocs.getDistribution(), TimeDistTopicLocs.getDistribution());
    }

    public static LptaPattern[] analyze(LptaResult result) {
        return new LptaPattern[0];
    }
}
