package edu.ntnu.app.psta;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        System.out.println("Initializing...");
        Docs.initialize("../dataset1000.txt");

        System.out.println("Executing...");
        PstaResult pattern = PSTA.execute(2);

        System.out.println("Analyzing...");
        PstaPattern[] patterns = PSTA.analyze(pattern);
        if (patterns.length == 0) {
            System.out.println("NO RESULTS");
        } else {
            for (PstaPattern p : patterns) {
                System.out.println(p);
            }
        }
    }
}
