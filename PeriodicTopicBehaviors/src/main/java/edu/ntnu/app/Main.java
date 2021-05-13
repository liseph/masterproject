package edu.ntnu.app;

import java.io.File;
import java.io.IOException;

public class Main {

    public static int nITERATIONS;

    public static void main(String[] args) {

        try {
            // Format of input is:
            // java -jar app.jar [alg_id] [test_id] [n_docs] [n_iterations] [path_in] [path_out]
            //      * alg_id = 0 for GeoLPTA, 1 for TopicPeriodica and 2 for PSTA+.
            //      * test_id = 0 for simple run, 1 for varying the number of topics and 2 for varying the number of documents.
            //      * n_docs = total number of documents in the input
            //      * n_iterations = number of iterations per setting to time the algorithm
            //      * path_in = path to input file
            //      * path_out = output file name

            if (args.length != 6) {
                System.out.println("Wrong number of args");
                System.exit(0);
            }
            int algorithm = Integer.parseInt(args[0]);
            int testType = Integer.parseInt(args[1]);
            int nDocs = Integer.parseInt(args[2]);
            nITERATIONS = Integer.parseInt(args[3]);
            String in = args[4];
            String out = args[5];

            // Select algorithm
            Algorithm alg;
            switch (algorithm) {
                case 0: {
                    System.out.println("Algorithm: GeoLPTA.");
                    alg = new edu.ntnu.app.lpta.Main();
                    break;
                }
                case 1: {
                    System.out.println("Algorithm: TopicPeriodica.");
                    alg = new edu.ntnu.app.periodica.Main();
                    break;
                }
                case 2: {
                    System.out.println("Algorithm: PSTA+.");
                    alg = new edu.ntnu.app.psta.Main();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid choice of algorithm. Invalid value: " + algorithm);
            }

            // Run based on test type
            new File("out").mkdir();
            switch (testType) {
                case 0: {
                    System.out.println("Run type: simple run.");
                    alg.run(nDocs, in, out);
                    break;
                }
                case 1: {
                    System.out.println("Run type: varying the number of topics.");
                    for (int nTopics = 1; nTopics < 11; nTopics++) {
                        alg.run(nDocs, in, out, nTopics);
                    }
                    break;
                }
                case 2: {
                    System.out.println("Run type: varying the number of documents.");
                    for (double docShare : new double[]{0.01, 0.1, 0.2, 0.5, 0.7, 1.0}) {
                        alg.run(nDocs, in, out, docShare);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid choice of test type. Invalid value: " + algorithm);
                }
            }
        } catch (IOException e) {
            System.out.println("Something went wrong with the IO...");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid algorithm value type, test value type, number of docs or number of iterations. Must be Integer.");
            e.printStackTrace();
        }

    }
}
