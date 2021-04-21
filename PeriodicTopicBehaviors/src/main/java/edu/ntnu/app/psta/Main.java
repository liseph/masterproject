package edu.ntnu.app.psta;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("LPTA");

        System.out.println("Initializing...");
        PstaDocs.initialize("../dataset1000.txt");

        System.out.println("Executing...");
        PstaResult pattern = Psta.execute(2);

        System.out.println("Analyzing...");
        PstaPattern[] patterns = Psta.analyze(pattern);
        if (patterns.length == 0) {
            System.out.println("NO RESULTS");
        } else {
            System.out.println("RESULTS:");
            for (PstaPattern p : patterns) {
                System.out.println(p);
            }
        }
    }
}
