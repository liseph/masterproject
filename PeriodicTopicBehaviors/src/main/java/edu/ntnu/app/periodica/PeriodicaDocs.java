package edu.ntnu.app.periodica;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;
import edu.ntnu.app.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PeriodicaDocs extends Docs {

    private static TimestampDocument[] tsDocs;
    private static ReferenceSpot[] referenceSpots;
    private static boolean docsAreDivided = false;

    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
    }

    public static double[][] getXYValues() {
        return docs.stream()
                .map(Document::getLocationId)
                .map(PeriodicaDocs::getLocation)
                .map(loc -> new double[]{loc.getLongitude(), loc.getLatitude()})
                .toArray(double[][]::new);
    }

    // Function to be called only once after generating the reference spots. Will divide the documents by timestamp and reference spot.
    public static void divideDocsByTimestampAndReferenceSpot(ReferenceSpot[] spots) {
        if (docsAreDivided) return;
        // Store docs in collections per timeslot and reference spot, they are already sorted by ts.
        referenceSpots = spots;
        tsDocs = new TimestampDocument[nTimeslots()];
        for (int t = 0, d = 0; t < nTimeslots() && d < nDocuments(); t++) {
            tsDocs[t] = new TimestampDocument();
            Document doc = getDoc(d);
            while (d < nDocuments() && doc.getTimestampId() == t) {
                int o = getReferenceSpotId(doc.getLocationId());
                tsDocs[t].addDoc(doc, o);
                d++;
            }
        }
        docsAreDivided = true;
    }

    private static int getReferenceSpotId(int l) {
        Location loc = getLocation(l);
        for (int o = 1; o < PeriodicaDocs.nRefSpots(); o++) {
            if (referenceSpots[o].containsPoint(loc.getLongitude(), loc.getLatitude())) {
                return o;
            }
        }
        return 0; // Id of reference spot representation of all areas not covered by reference spots.
    }

    public static String[] getTextsPerTsPerRefSpot() {
        return Arrays.stream(tsDocs).flatMap(TimestampDocument::getTexts).toArray(String[]::new);
    }

    public static int nRefSpots() {
        return referenceSpots.length;
    }
}

// A class to store all documents within the same timestamp and reference spot
class TimestampDocument {
    private final List<List<Document>> tsoDocs; // [ref spot][doc], ref spot = 0 is background spot

    public TimestampDocument() {
        this.tsoDocs = new ArrayList<>();
        for (int i = 0; i < PeriodicaDocs.nRefSpots(); i++) tsoDocs.add(new ArrayList<>());
    }

    public void addDoc(Document doc, int o) {
        tsoDocs.get(o).add(doc);
    }

    private String getTextPerRefSpot(int o) {
        return tsoDocs.get(o).stream().map(Document::getTerms).collect(Collectors.joining(". "));
    }

    public Stream<String> getTexts() {
        return IntStream.range(0, tsoDocs.size()).mapToObj(this::getTextPerRefSpot);
    }
}
