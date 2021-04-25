package edu.ntnu.app.periodica;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("PERIODICA");

        System.out.println("Initializing...");
        PeriodicaDocs.initialize("../dataset1000.txt");

        System.out.println("Executing...");
        Object[] patterns = Periodica.execute();

        if (patterns.length == 0) {
            System.out.println("NO RESULTS");
        } else {
            System.out.println("RESULTS:");
            for (Object p : patterns) {
                System.out.println(p);
            }
        }
    }
}
