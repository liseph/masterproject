package edu.ntnu.app.periodica;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class Topics {
    private static double[][][] topicDistributions; // [topic][ref spot][timestamp]
    private static ParallelTopicModel model;
    private static Alphabet dataAlphabet;

    public static void analyzeTopics() throws IOException {
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));
        // One doc = all docs within time t and ref spot o in 1D array, so docs[i] => i = o*len(t) + t.
        String[] docs = PeriodicaDocs.getTextsPerTsPerRefSpot();
        instances.addThruPipe(new StringArrayIterator(docs));

        model = new ParallelTopicModel(Periodica.nTOPICS, 1.0, 0.01);
        model.setRandomSeed(1000);
        model.addInstances(instances);
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(1500);
        model.estimate();

        dataAlphabet = instances.getDataAlphabet();

        // Estimate the topic distribution per document, given the current Gibbs state.
        int nTs = PeriodicaDocs.nTimeslots();
        int nO = PeriodicaDocs.nRefSpots();
        int nZ = Periodica.nTOPICS;
        double[][][] topicProbabilities = new double[nTs][nO][nZ];
        for (int d = 0; d < instances.size(); d++) {
            for (int t = 0; t < nTs; t++) {
                for (int o = 0; o < nO; o++) {
                    topicProbabilities[t][o] = model.getTopicProbabilities(d);
                    d++;
                }
            }
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

    // Define the presence of a topic to be 1 if its probability is greater than 1/nTopics
    public static double[] getTopicPresence(int z, int o) {
        double threshold = 1.0 / model.numTopics;
        return Arrays.stream(topicDistributions[z][o]).map(val -> val > Periodica.TOPIC_PRESENCE_LIM ? val : 0).toArray();
    }

    public static int[][] getSymbolizedSequence(Integer topicId, List<Integer> referenceSpots) {
        int[][] result = new int[PeriodicaDocs.nTimeslots()][];
        for (int t = 0; t < PeriodicaDocs.nTimeslots(); t++) {
            int finalT = t;
            result[t] = referenceSpots.stream()
                    .filter(o -> topicDistributions[topicId][o][finalT] > Periodica.TOPIC_PRESENCE_LIM)
                    .mapToInt(i -> i)
                    .toArray();
            if (result[t].length == 0) result[t] = new int[]{0}; // All areas outside ref spots
        }
        return result;
    }

    public static String getTopicString(Integer topic) {
        Iterator<IDSorter> iterator = model.getSortedWords().get(topic).iterator();
        int rank = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        while (iterator.hasNext() && rank < 10) {
            IDSorter idCountPair = iterator.next();
            sb.append(dataAlphabet.lookupObject(idCountPair.getID()));
            sb.append(": ");
            sb.append(idCountPair.getWeight());
            sb.append(", ");
            rank++;
        }
        sb.append("}");
        return sb.toString();
    }
}
