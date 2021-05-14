package edu.ntnu.app.psta;

public class LatentWordByTL {

    private static double[][] latentWordByTL;
    private static int nTopics;
    private static boolean converges;

    public static void initialize(int nTopics_) {
        nTopics = nTopics_;
        converges = false;
        latentWordByTL = new double[PstaDocs.nDocuments()][nTopics];
    }

    public static boolean hasConverged() {
        return converges;
    }

    public static void update() {
        converges = true;
        for (int d = 0; d < PstaDocs.nDocuments(); d++) {
            for (int z = 0; z < nTopics; z++) {
                double numerator = calc(z, d);
                double denominator = ((1 - Psta.LAMBDA_TL) * TopicDistDoc.get(d, z) + numerator);
                double newVal = denominator != 0 ? numerator / denominator : 0;
                for (int w = 0; w < PstaDocs.nWords(); w++) {
                    converges = converges && Math.abs(newVal - latentWordByTL[d][z]) < Psta.CONVERGES_LIM;
                    latentWordByTL[d][z] = newVal;
                }
            }
        }
    }

    private static double calc(int z, int d) {
        return Psta.LAMBDA_TL * TopicDistTL
                .get(PstaDocs.getDoc(d).getLocationId(), PstaDocs.getDoc(d).getTimestampId(), z);
    }

    public static double get(int docIndex, int wordIndex, int topicIndex) {
        return latentWordByTL[docIndex][topicIndex];
    }

    public static void clear() {
        latentWordByTL = null;
        nTopics = 0;
        converges = false;
    }
}