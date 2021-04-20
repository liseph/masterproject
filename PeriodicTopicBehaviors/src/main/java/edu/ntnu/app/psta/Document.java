package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.Stream;

public class Document {
    private static int idCount = 0;

    private final int id;
    private final int locationId;
    private final long timestamp;
    private final String[] terms;
    private int timestampIndex = -1;
    private int[] termIndices;

    public Document(int locationId, long timestamp, String[] terms) {
        this.id = idCount++;
        this.locationId = locationId;
        this.timestamp = timestamp;
        this.terms = terms;
    }

    public int getLocationId() {
        return locationId;
    }

    public int getTimestampId() {
        if (timestampIndex != -1)
            return timestampIndex;
        timestampIndex = Docs.getTimestampIndex(timestamp);
        return timestampIndex;
    }

    public Stream<String> getTerms() {
        return Arrays.stream(terms);
    }

    public boolean hasTimeAndLoc(int t, int l) {
        return t == getTimestampId() && l == locationId;
    }

    public int getId() {
        return id;
    }

    public int[] getTermIndices() {
        return termIndices;
    }

    public void setTermIndices(int[] docTermIndices) {
        this.termIndices = docTermIndices;
    }
}
