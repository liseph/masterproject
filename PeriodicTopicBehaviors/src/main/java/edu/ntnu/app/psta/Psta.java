package edu.ntnu.app.psta;

import edu.ntnu.app.autoperiod.FindPeriodsInTimeseries;
import edu.ntnu.app.autoperiod.Timeseries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Psta {

    // Constants
    public static final Float LAMBDA_B = 0.9f; // Empirically, a suitable λB for blog documents can be chosen between 0.9 and 0.95.
    public static final Float LAMBDA_TL = 0.5f; // Empirically, a suitable λTL for blog documents can be chosen between 0.5 and 0.7.
    public static final Float EPSILON = 1E-2f;
    public static final Float HOUR_MS = 3.6E+6f;
    public static final Float DAY_MS = 8.64E+7f;
    public static final Float TIME_CONVERT = DAY_MS;


    static public PstaResult execute(int nTopics) {
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

        boolean converged = false;
        //while (!converged) {
        System.out.println("START");
        for (int i = 0; i < 20; i++) {
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
        PstaDocs.nWords();

        return new PstaResult(themes, topicDistDocs, topicDistTLs);
    }

    // NOTE: Right now, the time series periodicity detection algorithm expects a regularily sampled time series.
    // Our PstaResult is NOT regularily sampled. This is because in these points, we only got 0 anyways, so it just
    // took a lot of space. Consider if this is a problem. I read that a few missing values is not a problem, and that
    // by filling them in, you make it worse... In theory, there shouldn't be that many missing values as we have a
    // large dataset.
    static public PstaPattern[] analyze(PstaResult pattern) {
        Map<Float, Map<Integer, PstaPattern>> patterns = new HashMap<>();
        for (int l = 0; l < PstaDocs.nLocations(); l++) {
            TopicDistTL topicDistTL = (TopicDistTL) pattern.getTopicDistTLs().get(l);
            for (int z = 0; z < pattern.nTopics(); z++) {
                int finalZ = z;
                int finalL = l;
                Float[] themeLifeCycle = IntStream
                        .range(0, PstaDocs.nTimeslots())
                        .mapToObj(t -> calc(topicDistTL, t, finalL, finalZ))
                        .toArray(Float[]::new);
                Float denominator = Arrays.stream(themeLifeCycle).reduce(0f, Float::sum);
                themeLifeCycle = Arrays.stream(themeLifeCycle).map(p -> p / denominator).toArray(Float[]::new);
                Float[] periods = FindPeriodsInTimeseries.execute(new Timeseries(themeLifeCycle, 1));
                for (Float p : periods) {
                    patterns.putIfAbsent(p, new HashMap<>());
                    patterns.get(p).putIfAbsent(z, new PstaPattern(p, 0, z)); // TODO: Fix offset. How do we get that?
                    patterns.get(p).get(z).addLocation(l);
                }
            }
        }
        return patterns.values().stream().map(m -> m.values()).toArray(PstaPattern[]::new);
    }

    static private Float calc(Variable topicDistTL, int t, int l, int z) {
        return topicDistTL.get(t, z) * PstaDocs.getSumWordCount(t, l) / PstaDocs.getSumWordCount();
    }
}