package edu.ntnu.app.periodica;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Topics {
    private static double[][][] topicDistributions; // [topic][ref spot][timestamp]
    private static ParallelTopicModel model;

    public static void analyzeTopics() throws IOException {
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));
        // One doc = all docs within time t and ref spot o so docs[i] => i = o*len(t) + t.
        String[] docs = PeriodicaDocs.getTextsPerTsPerRefSpot();
        instances.addThruPipe(new StringArrayIterator(docs));

        model = new ParallelTopicModel(Periodica.nTOPICS, 1.0, 0.01);
        model.addInstances(instances);
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(50);
        model.estimate();

        // Estimate the topic distribution per document, given the current Gibbs state.
        int nTs = PeriodicaDocs.nTimeslots();
        int nO = PeriodicaDocs.nRefSpots();
        int nZ = Periodica.nTOPICS;
        double[][][] topicProbabilities = new double[nTs][nO][nZ];
        for (int d = 0; d < instances.size(); d++) {
            int oIndex = d / nTs;
            int tIndex = d - oIndex * nTs;
            topicProbabilities[tIndex][oIndex] = model.getTopicProbabilities(d);
        }

        // Transpose array from [timestamp][ref spot][topic] to [topic][ref spot][timestamp]
        topicDistributions = new double[nZ][nO][nTs];
        for (int z = 0; z < nZ; z++) {
            for (int o = 0; o < nO; o++) {
                for (int t = 0; t < nTs; t++) {
                    topicDistributions[z][o][t] = topicProbabilities[t][o][z];
                }
            }
        }
    }

    // Instead of binary as in the original Periodica, we use probability of the topic in that ref spot in that ts.
    public static double[] getTopicPresence(int z, int o) {
        return topicDistributions[z][o];
    }

    public static int[][] getSymbolizedSequence(Integer topicId, List<Integer> referenceSpots) {
        int[][] result = new int[PeriodicaDocs.nTimeslots()][];
        for (int t = 0; t < PeriodicaDocs.nTimeslots(); t++) {
            int finalT = t;
            result[t] = referenceSpots.stream()
                    .filter(o -> topicDistributions[topicId][o][finalT] > Periodica.EPSILON)
                    .mapToInt(i -> i)
                    .toArray();
        }
        return result;
    }
}
