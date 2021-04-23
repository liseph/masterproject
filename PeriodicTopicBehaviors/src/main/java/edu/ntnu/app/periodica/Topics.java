package edu.ntnu.app.periodica;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Topics {
    private final double[][][] topicDistributions;
    private final ParallelTopicModel model;

    public Topics(ParallelTopicModel model, double[][][] topicDistributions) {
        this.model = model;
        this.topicDistributions = topicDistributions;
    }

    public static Topics analyzeTopics() throws IOException {

        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        InstanceList instances = new InstanceList (new SerialPipes(pipeList));
        // One doc = all docs within time t and ref spot o so docs[i] => i = o*len(t) + t.
        String[] docs = PeriodicaDocs.getTextsPerTsPerRefSpot();
        instances.addThruPipe(new StringArrayIterator(docs));

        ParallelTopicModel model = new ParallelTopicModel(Periodica.nTOPICS, 1.0, 0.01);
        model.addInstances(instances);
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(50);
        model.estimate();

        // Estimate the topic distribution per document, given the current Gibbs state.
        int t = PeriodicaDocs.nTimeslots();
        int o = PeriodicaDocs.nRefSpots();
        double[][][] topicDistributions = new double[t][o][instances.size()];
        for (int d = 0; d < instances.size(); d++) {
            int oIndex = d / t;
            int tIndex = d - oIndex * t;
            topicDistributions[tIndex][oIndex] = model.getTopicProbabilities(d);
        }

        return new Topics(model, topicDistributions);
    }

    public static Float[] generateTopicPresence(int z, int o) {
        return new Float[0];
    }
}
