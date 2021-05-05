package edu.ntnu.app.periodica;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("PERIODICA");

        System.out.println("Initializing...");
        PeriodicaDocs.initialize("../datasetSynth1000.txt");

        System.out.println("Executing...");
        PeriodicaResult[] patterns = Periodica.execute();

        if (patterns.length == 0) {
            System.out.println("NO RESULTS");
        } else {
            System.out.println("RESULTS:");
            for (PeriodicaResult p : patterns) {
                System.out.println(p);
            }
            System.out.println("Ref points:");
            for (ReferenceSpot s : Periodica.referenceSpots) {
                System.out.println("" + s.getId() + s.getLocationsInRefSpot().toString());

            }
        }
    }
}
