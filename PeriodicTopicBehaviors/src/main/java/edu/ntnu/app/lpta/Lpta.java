package edu.ntnu.app.lpta;

public class Lpta {


    public static LptaResult execute(int nPeriodicTopics, int nBurstyTopics) {

        // Latent variable
        LatentWordByTopics latentWordByTopics = LatentWordByTopics.initialize(nPeriodicTopics, nBurstyTopics);

        // Unknown variables
        Topics topics = Topics.initialize(nPeriodicTopics, nBurstyTopics, latentWordByTopics);
        TopicDistDocs topicDistDocs = TopicDistDocs.initialize(nPeriodicTopics, nBurstyTopics, latentWordByTopics);
        TimeDistTopicLocs timeDistTopicLocs = TimeDistTopicLocs.initialize(nPeriodicTopics, nBurstyTopics, latentWordByTopics);

        // Connect
        latentWordByTopics.setVars(topics, topicDistDocs, timeDistTopicLocs);

        boolean converged = false;
        System.out.println("START EM ALGORITHM");
        for (int i = 0; i < 10; i++) {
            System.out.format("Round %d: ", i + 1);
            long startTime = System.nanoTime();

            // E-step
            latentWordByTopics.update();
            boolean b1 = latentWordByTopics.hasConverged();
            long t1 = System.nanoTime();
            System.out.format("[ %d:%b, ", (t1 - startTime) / 1000000, b1);

            // M-step
            topics.update();
            boolean b2 = topics.hasConverged();
            long t2 = System.nanoTime();
            System.out.format("%d:%b, ", (t2 - t1) / 1000000, b2);

            topicDistDocs.update();
            boolean b3 = topicDistDocs.hasConverged();
            long t3 = System.nanoTime();
            System.out.format("%d:%b, ", (t3 - t2) / 1000000, b3);

            timeDistTopicLocs.update();
            boolean b4 = timeDistTopicLocs.hasConverged();
            long t4 = System.nanoTime();
            System.out.format("%d:%b, ", (t4 - t3) / 1000000, b4);

            converged = b1 && b2 && b3 && b4;
            if (converged) break;
        }


        return new LptaResult(topics, topicDistDocs, timeDistTopicLocs);
    }

    public static LptaPattern[] analyze(LptaResult result) {
        return new LptaPattern[0];
    }
}
