package edu.ntnu.app.lpta;

import java.io.IOException;
import java.util.List;

public class Main {
    //public static final double[] PERIODS = new double[]{4, 7, 25};
    public static final double[] PERIODS = new double[]{7, 15};
    public static final int nTOPICS = PERIODS.length;

    public static void main(String[] args) throws IOException {
        System.out.println("GeoLPTA");

        System.out.println("Initializing...");
        LptaDocs.initialize("../datasets/datasetSynth1000Improved.txt");

        System.out.println("Executing...");
        Lpta.execute(nTOPICS, PERIODS);

        System.out.println("Analyzing...");
        List<LptaPattern> patterns = Lpta.analyze(nTOPICS, PERIODS);

        if (patterns.isEmpty()) {
            System.out.println("NO RESULTS");
        } else {
            System.out.println("RESULTS:");
            for (LptaPattern p : patterns) {
                System.out.println(p);
            }
        }
    }
}
