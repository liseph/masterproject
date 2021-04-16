package edu.ntnu.app.psta;

public class PSTA {

    // Constants
    public static final double LAMBDA_B = 0.5; // Empirically, a suitable λB for blog documents can be chosen between 0.9 and 0.95.
    public static final double LAMBDA_TL = 0.5; // Empirically, a suitable λTL for blog documents can be chosen between 0.5 and 0.7.
    public static final double EPSILON = 1E-3;
    public static final double HOUR_MS = 3.6E+6;
    public static final double DAY_MS = 8.64E+7;
    public static final double TIME_CONVERT = DAY_MS;


    static public PstaPattern execute(int nTopics) {
        // Unknown values
        VariableList themes = Theme.generateEmptyThemes(nTopics);
        VariableList topicDistDocs = TopicDistDoc.generateEmptyTopicDist(nTopics);
        VariableList topicDistTLs = TopicDistTL.generateEmptyTopicDist(nTopics);

        // Latent variables
        VariableList latentWordByTopic = LatentWordByTopic.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs);
        VariableList latentWordByTL = LatentWordByTL.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs);

        // Connect the unknown variables and the latent variables to they can use each other to update themselves
        themes.setVars(latentWordByTopic, latentWordByTL);
        topicDistDocs.setVars(latentWordByTopic, latentWordByTL);
        topicDistTLs.setVars(latentWordByTopic, latentWordByTL);

        Docs.nDocuments();
        boolean converged = false;
        //while (!converged) {
        System.out.println("START");
        for (int i = 0; i < 400; i++) {
            System.out.format("Round %d: ", i+1);
            long startTime = System.nanoTime();
            // E-step
            latentWordByTopic.updateAll();
            boolean b1 = latentWordByTopic.hasConverged();
            long t1 = System.nanoTime();
            System.out.format("[ %d:%b, ", (t1-startTime)/1000000,b1);

            latentWordByTL.updateAll();
            boolean b2 = latentWordByTL.hasConverged();
            long t2 = System.nanoTime();
            System.out.format("%d:%b, ", (t2-t1)/1000000, b2);

            // M-step
            themes.updateAll();
            boolean b3 = themes.hasConverged();
            long t3 = System.nanoTime();
            System.out.format("%d:%b, ", (t3-t2)/1000000, b3);

            topicDistDocs.updateAll();
            boolean b4 = topicDistDocs.hasConverged();
            long t4 = System.nanoTime();
            System.out.format("%d:%b, ", (t4-t3)/1000000, b4);

            topicDistTLs.updateAll();
            boolean b5 = topicDistTLs.hasConverged();
            long t5 = System.nanoTime();
            System.out.format("%d:%b ]\n", (t5-t4)/1000000, b5);

            // Check for convergence
            converged = b1 &&
                    b2 &&
                    b3 &&
                    b4 &&
                    b5;
            if (converged) break;
        }
        System.out.println(converged);
        Docs.nWords();

        return new PstaPattern(themes, topicDistDocs, topicDistTLs);
    }
}
