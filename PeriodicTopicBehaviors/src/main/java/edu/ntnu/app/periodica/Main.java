package edu.ntnu.app.periodica;

import edu.ntnu.app.Algorithm;

import java.io.IOException;
import java.io.PrintWriter;

public class Main extends Algorithm {

    private PeriodicaResult[] patterns;

    @Override
    protected void clearAll() {
        Periodica.referenceSpots = null;
        patterns = null;
        ReferenceSpot.clear();
        Topics.clear();
        PeriodicaDocs.clear();
    }

    @Override
    protected void printResults(PrintWriter outRes) {
        if (patterns.length == 0) {
            outRes.println("NO RESULTS");
        } else {
            outRes.println("RESULTS:");
            for (PeriodicaResult p : patterns) {
                outRes.println(p);
            }
            outRes.println("Ref points:");
            for (ReferenceSpot s : Periodica.referenceSpots) {
                outRes.println("" + s.getId() + s.getLocationsInRefSpot().toString());
            }
        }
    }

    @Override
    protected void analyze() {
        // No analyze phase in Periodica.
    }

    @Override
    protected boolean stop() {
        return false;
    }

    @Override
    protected void execute() throws IOException {
        patterns = Periodica.execute();
    }

    @Override
    protected void initialize() throws IOException {
        PeriodicaDocs.initialize(inPath, nDocs);
    }
}
