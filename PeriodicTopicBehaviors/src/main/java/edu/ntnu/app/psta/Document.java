package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.Stream;

public class Document {
    private static int idCount = 0;

    private final int id;
    private final int locationId;
    private final long timestamp;
    private final String[] terms;
    private Integer[] termIndices;

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
        return Docs.getTimestampId(timestamp);
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

    public void setTermIndices(Integer[] docTermIndices) {
        this.termIndices = docTermIndices;
    }

    public Integer[] getTermIndices() {
        return termIndices;
    }
}
