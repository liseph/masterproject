package edu.ntnu.app.psta;

import edu.ntnu.app.lpta.LptaDocs;

public class LatentWordByTopic {

    private static double[][][] latentWordByTopic;
    private static int nTopics;
    private static boolean converges;

    public static void initialize(int nTopics_) {
        nTopics = nTopics_;
        converges = false;
        int nDocs = PstaDocs.nDocuments();
        latentWordByTopic = new double[nDocs][][];
        for (int d = 0; d < PstaDocs.nDocuments(); d++) {
            int nTermsInDoc = PstaDocs.getDoc(d).getTermIndices().length;
            latentWordByTopic[d] = new double[nTermsInDoc][nTopics];
        }
    }

    public static void update() {
        converges = true;
        for (int d = 0; d < PstaDocs.nDocuments(); d++) {
            int[] termIndices = PstaDocs.getDoc(d).getTermIndices();
            for (int wIndex = 0; wIndex < termIndices.length; wIndex++) {
                int w = termIndices[wIndex];
                double[] numerator = new double[nTopics];
                double sum = 0;
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = calcProbTopicByDocAndTL(z, d, w);
                    sum += numerator[z];
                }
                double denominator = Psta.LAMBDA_B * PstaDocs.backgroundTheme[w] +
                        (1 - Psta.LAMBDA_B) * sum;
                if (denominator == 0)
                    throw new IllegalStateException("NaN LatentWordByTopic: Should not happen.");
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = (1 - Psta.LAMBDA_B) * numerator[z] / denominator;
                    converges = converges && Math.abs(numerator[z] - latentWordByTopic[d][wIndex][z]) < Psta.CONVERGES_LIM;
                }
                latentWordByTopic[d][wIndex] = numerator;
            }
        }
    }

    private static double calcProbTopicByDocAndTL(int z, int d, int w) {
        return Theme.get(z, w) *
                ((1 - Psta.LAMBDA_TL) * TopicDistDoc.get(d, z) +
                        Psta.LAMBDA_TL *
                                TopicDistTL.get(PstaDocs.getDoc(d).getLocationId(), PstaDocs.getDoc(d).getTimestampId(), z));
    }

    public static double get(int d, int w, int z) {
        int[] termIndices = LptaDocs.getDoc(d).getTermIndices();
        int wIndex = -1;
        for (int i = 0; i < termIndices.length; i++) {
            if (termIndices[i] == w) {
                wIndex = i;
                break;
            }
        }
        return wIndex != -1 ? latentWordByTopic[d][wIndex][z] : 0;
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static void clear() {
        latentWordByTopic = null;
        nTopics = 0;
        converges = false;
    }
}
