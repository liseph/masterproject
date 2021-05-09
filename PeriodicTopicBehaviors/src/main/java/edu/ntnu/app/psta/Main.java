package edu.ntnu.app.psta;

import java.io.IOException;
import java.util.Random;

public class Main {

    public static final int nTOPICS = 2;
    private static final int nITERATIONS = 1;
    private static final long INIT_SEED = 1000;

    public static void main(String[] args) throws IOException {
        System.out.println("PSTA");

        System.out.println("Initializing...");
        PstaDocs.initialize("../datasets/datasetSynth1000Improved.txt");

        Random r = new Random(INIT_SEED);
        for (int i = 0; i < nITERATIONS; i++) {
            System.out.format("ITERATION %d\n", i);

            System.out.println("Executing...");
            PstaResult pattern = Psta.execute(nTOPICS, r.nextLong());

            System.out.println("Analyzing...");
            PstaPattern[] patterns = Psta.analyze(pattern);
            if (patterns.length == 0) {
                System.out.println("NO RESULTS");
            } else {
                System.out.println("RESULTS:");
                for (PstaPattern p : patterns) {
                    System.out.println(p);
                }
                System.out.println("THEMES:");
                for (Variable theme : Psta.themes) {
                    System.out.println(theme);
                }
            }
            Psta.clearAll();
        }

    }
}
