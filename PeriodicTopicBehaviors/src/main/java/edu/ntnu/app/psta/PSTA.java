package edu.ntnu.app.psta;

public class PSTA {

    // Constants
    public static final double LambdaB = 0.9; // Empirically, a suitable λB for blog documents can be chosen between 0.9 and 0.95.
    public static final double LambdaTL = 0.5; // Empirically, a suitable λTL for blog documents can be chosen between 0.5 and 0.7.
    public static final double EPSILON = 1E-10;


    static public PstaPattern execute(Documents docs, int nTopics) {
        // Unknown values
        VariableList themes = Theme.generateEmptyThemes(nTopics, docs);
        VariableList topicDistDocs = TopicDistDoc.generateEmptyTopicDist(nTopics, docs);
        VariableList topicDistTLs = TopicDistTL.generateEmptyTopicDist(nTopics, docs);

        // Latent variables
        VariableList latentWordByTopic = LatentWordByTopic.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs, docs);
        VariableList latentWordByTL = LatentWordByTL.generateEmptyTopicDist(themes, topicDistDocs, topicDistTLs, docs);

        // Connect the unknown variables and the latent variables to they can use each other to update themselves
        themes.setVars(latentWordByTopic, latentWordByTL);
        topicDistDocs.setVars(latentWordByTopic, latentWordByTL);
        topicDistTLs.setVars(latentWordByTopic, latentWordByTL);


        boolean converged = true;
        while (!converged) {
            // E-step
            latentWordByTopic.updateAll();
            latentWordByTL.updateAll();

            // M-step
            themes.updateAll();
            topicDistDocs.updateAll();
            topicDistTLs.updateAll();

            // Check for convergence
            converged = latentWordByTopic.hasConverged() &&
                    latentWordByTL.hasConverged() &&
                    themes.hasConverged() &&
                    topicDistDocs.hasConverged() &&
                    topicDistTLs.hasConverged();
        }

        return new PstaPattern(themes, topicDistDocs, topicDistTLs);
    }
}
