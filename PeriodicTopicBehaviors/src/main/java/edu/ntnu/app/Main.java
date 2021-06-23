package edu.ntnu.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    public static int nITERATIONS = 1;
    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static void main(String[] args) {

        try {
            // Format of input is:
            // java -jar app.jar [alg_id] [test_id] [n_docs] [n_iterations] [path_in] [path_out]
            //      * alg_id = 0 for GeoLPTA, 1 for TopicPeriodica and 2 for PSTA+.
            //      * test_id = 0 for simple run, 1 for varying the number of topics,
            //        2 for varying the number of documents and 3 for timing reading the input.
            //      * n_docs = number of documents to read in the input
            //      * n_iterations = number of iterations per setting to time the algorithm
            //      * n_topics = number of topics, ignored if test_id = 1.
            //      * path_in = path to input file
            //      * path_out = output file name

            if (args.length != 7) {
                System.out.println("Wrong number of args");
                System.exit(0);
            }
            int algorithm = Integer.parseInt(args[0]);
            int testType = Integer.parseInt(args[1]);
            int nDocs = Integer.parseInt(args[2]);
            nITERATIONS = Integer.parseInt(args[3]);
            int in_nTopics = Integer.parseInt(args[4]);
            String in = args[5];
            String out = args[6];

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
                    alg.run(nDocs, in, out, in_nTopics);
                    break;
                }
                case 1: {
                    System.out.println("Run type: varying the number of topics.");
                    for (int nTopics = 2; nTopics < 11; nTopics += 2) {
                        alg.run(nDocs, in, out, nTopics);
                    }
                    break;
                }
                case 2: {
                    System.out.println("Run type: varying the number of documents.");
                    for (double docShare : new double[]{0.01, 0.2, 0.5, 1.0}) {
                        alg.run(nDocs, in, out, docShare);
                    }
                    break;
                }
                case 3: {
                    System.out.println("Run type: time reading docs.");
                    FileOutputStream fileOutTime = new FileOutputStream("out/time_init_" + out);
                    PrintWriter outTime = new PrintWriter(fileOutTime);
                    outTime.print("[");
                    for (int i = 0; i < 21; i++) {
                        long start = System.nanoTime();
                        Docs.initialize(in, nDocs);
                        long end = System.nanoTime();
                        outTime.format("%d, ", end - start);
                        Docs.clear();
                    }
                    outTime.print("]\n");
                    outTime.close();
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
        System.out.println("Finished!");
        // Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        // Calculate the used memory
        long memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.format("Used memory is bytes: %d / %d \n", memory, runtime.totalMemory());
        System.out.println("Used memory is megabytes: "
                + bytesToMegabytes(memory));
    }
}
