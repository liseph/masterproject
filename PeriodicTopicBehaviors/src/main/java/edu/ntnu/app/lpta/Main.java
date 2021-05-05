package edu.ntnu.app.lpta;

import java.io.IOException;
import java.util.List;

public class Main {
    public static final int nTOPICS = 10;
    public static final double[] PERIODS = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 15};

    public static void main(String[] args) throws IOException {
        System.out.println("GeoLPTA");

        System.out.println("Initializing...");
        //LptaDocs.initialize("../datasetAllEn.txt");
        LptaDocs.initialize("../datasetSynth1000.txt");

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
