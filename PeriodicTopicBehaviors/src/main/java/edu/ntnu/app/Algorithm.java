package edu.ntnu.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

import static edu.ntnu.app.Main.nITERATIONS;

public abstract class Algorithm {

    public static int nTOPICS = 2;
    private static String filePathHead = "./out/";
    protected String inPath;
    protected String outPath;
    protected int nDocs;

    public static double[] generateRandomDistribution(int length) {
        double[] d = new Random().doubles(length, 0, 1).toArray();
        double total = Arrays.stream(d).sum();
        return Arrays.stream(d).map(v -> v / total).toArray();
    }

    public void run(int nDocs, String inPath, String outPath, double docShare) throws IOException {
        Docs.setDocShare(docShare);
        filePathHead = filePathHead + "ndoc_" + (nDocs * docShare) + "_";
        run(nDocs, inPath, outPath);
    }

    public void run(int nDocs, String inPath, String outPath, int nTopics) throws IOException {
        nTOPICS = nTopics;
        filePathHead = filePathHead + "ntop_" + nTopics + "_";
        run(nDocs, inPath, outPath);
    }

    public void run(int nDocs, String inPath, String outPath) throws IOException {
        this.inPath = inPath;
        this.outPath = outPath;
        this.nDocs = nDocs;

        String line = "===========================================================";
        System.out.format("%s\n#DOCS: %.1f, #TOPICS: %d, #ITERATIONS: %d\n%s\n",
                line, nDocs * Docs.docShare, nTOPICS, nITERATIONS, line);

        FileOutputStream fileOutRes = new FileOutputStream(filePathHead + "res_" + outPath);
        FileOutputStream fileOutTime = new FileOutputStream(filePathHead + "time_" + outPath);
        PrintWriter outRes = new PrintWriter(fileOutRes);
        PrintWriter outTime = new PrintWriter(fileOutTime);

        outTime.format("#DOCS: %.1f, #TOPICS: %d\n", nDocs * Docs.docShare, nTOPICS);

        Long[] init = new Long[nITERATIONS];
        Long[] execute = new Long[nITERATIONS];
        Long[] analyze = new Long[nITERATIONS];
        Long[] sum = new Long[nITERATIONS];

        for (int i = 0; i < nITERATIONS; i++) {
            System.out.format("------------------\nIteration %d\n------------------\n", i + 1);
            outRes.format("------------------\nIteration %d\n------------------\n", i + 1);
            outTime.format("------------------\nIteration %d\n------------------\n", i + 1);
            System.out.println("Initializing...");
            long startInit = System.nanoTime();
            initialize();
            long endInit = System.nanoTime();

            long startAlg = System.nanoTime();
            System.out.println("Executing...");
            execute();
            long midAlg = System.nanoTime();

            if (stop()) {
                System.out.println("Stopping due to convergence failure...");
                execute[i] = midAlg - startAlg;
                analyze[i] = null;
                sum[i] = init[i] + execute[i];
                outRes.println("NO RESULTS");
                outTime.format("[Init, execute, analyze, sum] = [%d, %d, %d, %d]\n", init[i], execute[i], analyze[i], sum[i]);
                outTime.flush();
                outRes.flush();
                clearAll();
                System.out.format("Finished iteration %d / %d.\n", i + 1, nITERATIONS);
                continue;
            }

            System.out.println("Analyzing...");
            analyze();
            long endAlg = System.nanoTime();

            init[i] = endInit - startInit;
            execute[i] = midAlg - startAlg;
            analyze[i] = endAlg - midAlg;
            sum[i] = init[i] + execute[i] + analyze[i];

            printResults(outRes);
            outTime.format("[Init, execute, analyze, sum] = [%d, %d, %d, %d]\n", init[i], execute[i], analyze[i], sum[i]);
            outTime.flush();
            outRes.flush();
            clearAll();
            System.out.format("Finished iteration %d / %d.\n", i + 1, nITERATIONS);
        }

        outTime.format("Init = %s\n", Arrays.toString(init));
        outTime.format("Execute = %s\n", Arrays.toString(execute));
        outTime.format("Analyze = %s\n", Arrays.toString(analyze));
        outTime.format("Total = %s\n", Arrays.toString(sum));

        outTime.close();
        outRes.close();
        nTOPICS = 2;
        filePathHead = "./out/";
        Docs.setDocShare(1.0);
    }

    protected abstract void clearAll();

    protected abstract void printResults(PrintWriter out);

    protected abstract void analyze();

    protected abstract boolean stop();

    protected abstract void execute() throws IOException;

    protected abstract void initialize() throws IOException;

}
