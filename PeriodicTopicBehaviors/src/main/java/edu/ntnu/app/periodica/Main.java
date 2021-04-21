package edu.ntnu.app.periodica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
//        System.out.println("PERIODICA");
//
//        System.out.println("Initializing...");
//        PeriodicaDocs.initialize("../dataset1000.txt");
//
//        System.out.println("Executing...");
//        Object[] patterns = Periodica.execute(2);
//
//        if (patterns.length == 0) {
//            System.out.println("NO RESULTS");
//        } else {
//            System.out.println("RESULTS:");
//            for (Object p : patterns) {
//                System.out.println(p);
//            }
//        }
        List<List<Integer>> o = new ArrayList<>();
        List<Integer> i = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            i = new ArrayList<>();
            o.add(i);
        }
        System.out.println(o);
    }
}
