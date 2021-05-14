package edu.ntnu.app.psta;

import edu.ntnu.app.Algorithm;

import java.io.IOException;
import java.io.PrintWriter;

public class Main extends Algorithm {

    private boolean converged;
    private PstaPattern[] patterns;

    @Override
    protected void clearAll() {
        converged = false;
        PstaDocs.clear();
        Psta.clearAll();
        patterns = null;
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
            outRes.println(Theme.getAsString());
        }
    }

    @Override
    protected void analyze() {
        patterns = Psta.analyze();
    }

    @Override
    protected boolean stop() {
        return !converged;
    }

    @Override
    protected void execute() {
        converged = Psta.execute(nTOPICS);
    }

    @Override
    protected void initialize() throws IOException {
        PstaDocs.initialize(inPath, nDocs);
    }
}
