package edu.ntnu.app.lpta;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("GeoLPTA");

        System.out.println("Initializing...");
        LptaDocs.initialize("../dataset1000.txt");

        System.out.println("Executing...");
        LptaResult result = Lpta.execute(2, 2);

        System.out.println("Analyzing...");
        LptaPattern[] patterns = Lpta.analyze(result);

        if (patterns.length == 0) {
            System.out.println("NO RESULTS");
        } else {
            System.out.println("RESULTS:");
            for (LptaPattern p : patterns) {
                System.out.println(p);
            }
        }
    }
}
