package edu.ntnu.app.lpta;

import edu.ntnu.app.Algorithm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class Main extends Algorithm {
    //public static final double[] PERIODS = new double[]{4, 7, 25};

    private static final double[] P = new double[]{7, 15, 4, 30, 90, 14, 5, 60, 182, 365};
    public static double[] PERIODS;

    private boolean converged;
    private List<LptaPattern> patterns;

    @Override
    protected void clearAll() {
        LptaDocs.clear();
        LatentWordByTopics.clear();
        Topics.clear();
        TopicDistDocs.clear();
        TimeDistTopicLocs.clear();
        converged = false;
        patterns = null;
    }

    @Override
    protected void printResults(PrintWriter outRes) {
        if (patterns.isEmpty()) {
            outRes.println("NO RESULTS");
        } else {
            outRes.println("RESULTS:");
            for (LptaPattern p : patterns) {
                outRes.println(p);
            }
        }
    }

    @Override
    protected void analyze() {
        patterns = Lpta.analyze(nTOPICS, PERIODS);
    }

    @Override
    protected boolean stop() {
        return !converged;
    }

    @Override
    protected void execute() {
        converged = Lpta.execute(nTOPICS, PERIODS);
    }

    @Override
    protected void initialize() throws IOException {
        PERIODS = Arrays.copyOf(P, nTOPICS);
        LptaDocs.initialize(inPath, nDocs);
    }

    public static void main(String[] args) throws IOException {
        Algorithm a = new edu.ntnu.app.lpta.Main();
        edu.ntnu.app.Main.nITERATIONS = 1;
        a.run(1000, "../datasets/datasetSynth1000_1.txt.gz", "outlpta.txt");
    }
}
