package edu.ntnu.app.lpta;

public class LatentWordByTopics {

    private static double[][][] latentWordByTopics;
    private static boolean converges;
    private static int nTopics;

    public static void initialize(int nPeriodicTopics) {
        nTopics = nPeriodicTopics + 1;
        converges = false;
        latentWordByTopics = new double[LptaDocs.nDocuments()][][];
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            int termIndices = LptaDocs.getDoc(d).getTermIndices().length;
            latentWordByTopics[d] = new double[termIndices][nTopics];
        }
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static void update() {
        converges = true;
        for (int d = 0; d < LptaDocs.nDocuments(); d++) {
            int[] termIndices = LptaDocs.getDoc(d).getTermIndices();
            for (int wIndex = 0; wIndex < termIndices.length; wIndex++) {
                int w = termIndices[wIndex];
                double[] numerator = new double[nTopics];
                double denominator = 0;
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = calc(d, w, z);
                    denominator += numerator[z];
                }
                double uniform = 1.0 / nTopics;
                for (int z = 0; z < nTopics; z++) {
                    numerator[z] = denominator != 0 ? numerator[z] / denominator : uniform;
                    converges = Math.abs(numerator[z] - latentWordByTopics[d][wIndex][z]) < Lpta.CONVERGES_LIM;
                }
                latentWordByTopics[d][wIndex] = numerator;
            }
        }
    }

    private static double calc(int d, int w, int z) {
        return TimeDistTopicLocs.get(z, d) * Topics.get(z, w) * TopicDistDocs.get(d, z);
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
        return wIndex != -1 ? latentWordByTopics[d][wIndex][z] : 0;
    }

    public static void clear() {
        latentWordByTopics = null;
        converges = false;
        nTopics = 0;
    }
}
