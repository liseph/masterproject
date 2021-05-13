package edu.ntnu.app.psta;

import edu.ntnu.app.Algorithm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class Main extends Algorithm {

    private static final long INIT_SEED = 1000;
    private final Random r = new Random();
    private PstaPattern[] patterns;
    private PstaResult pattern;

    @Override
    protected void clearAll() {
        PstaDocs.clear();
        Psta.clearAll();
    }

    @Override
    protected void printResults(PrintWriter outRes) {
        if (patterns.length == 0) {
            outRes.println("NO RESULTS");
        } else {
            outRes.println("RESULTS:");
            for (PstaPattern p : patterns) {
                outRes.println(p);
            }
            outRes.println("THEMES:");
            for (Variable theme : Psta.themes) {
                outRes.println(theme);
            }
        }
    }

    @Override
    protected void analyze() {
        patterns = Psta.analyze(pattern);
    }

    @Override
    protected boolean stop() {
        return pattern == null;
    }

    @Override
    protected void execute() throws IOException {
        pattern = Psta.execute(nTOPICS, r.nextLong());
    }

    @Override
    protected void initialize() throws IOException {
        PstaDocs.initialize(inPath, nDocs);
    }
}
