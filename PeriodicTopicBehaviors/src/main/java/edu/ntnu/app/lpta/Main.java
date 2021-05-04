package edu.ntnu.app.lpta;

import java.io.IOException;
import java.util.List;

public class Main {
    public static final int nTOPICS = 2;

    public static void main(String[] args) throws IOException {
        System.out.println("GeoLPTA");

        System.out.println("Initializing...");
        //LptaDocs.initialize("../datasetAllEn.txt");
        LptaDocs.initialize("../datasetSynth1000.txt");

        System.out.println("Executing...");
        double[] periods = new double[]{7, 15};
        Lpta.execute(nTOPICS, periods);

        System.out.println("Analyzing...");
        List<LptaPattern> patterns = Lpta.analyze(nTOPICS, periods);

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
