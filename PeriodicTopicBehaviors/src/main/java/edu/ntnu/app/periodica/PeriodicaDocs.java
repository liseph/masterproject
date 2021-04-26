package edu.ntnu.app.periodica;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;
import edu.ntnu.app.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
        Location[] locations = Docs.getLocations();
        return Arrays.stream(docs)
                .map(Document::getLocationId)
                .map(locId -> locations[locId])
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
            tsDocs[t] = new TimestampDocument(t);
            while (d < nDocuments() && docs[d].getTimestampId() == t) {
                int o = getReferenceSpotId(docs[d].getLocationId());
                tsDocs[t].addDoc(docs[d], o);
                d++;
            }
        }
        docsAreDivided = true;
    }

    private static int getReferenceSpotId(int l) {
        Location loc = locations.get(l);
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

    public static String getTextInTimestampAsOne(int t) {
        return tsDocs[t].getTextsAsOne();
    }

    public static int nRefSpots() {
        return referenceSpots.length;
    }
}

// A class to store all documents within the same timestamp and reference spot
class TimestampDocument {
    private final List<List<Document>> tsoDocs; // [ref spot][doc], ref spot = 0 is background spot
    private final int timestampId;

    public TimestampDocument(int timestampId) {
        this.tsoDocs = new ArrayList<>();
        for (int i = 0; i < PeriodicaDocs.nRefSpots(); i++) tsoDocs.add(new ArrayList<>());
        this.timestampId = timestampId;
    }

    public void addDoc(Document doc, int o) {
        tsoDocs.get(o).add(doc);
    }

    public List<Document> getDocs(int o) {
        return tsoDocs.get(o);
    }

    public List<Document> getDocsInRefSpot(int o) {
        return tsoDocs.get(o);
    }

    public int getTimestampId() {
        return timestampId;
    }

    public String getTextsAsOne() {
        return tsoDocs.stream().flatMap(Collection::stream).map(Document::getTerms).collect(Collectors.joining(". "));
    }

    private String getTextPerRefSpot(int o) {
        return tsoDocs.get(o).stream().map(Document::getTerms).collect(Collectors.joining(". "));
    }

    public Stream<String> getTexts() {
        return IntStream.range(0, tsoDocs.size()).mapToObj(this::getTextPerRefSpot);
    }
}
