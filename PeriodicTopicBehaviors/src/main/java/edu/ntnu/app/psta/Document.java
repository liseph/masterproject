package edu.ntnu.app.psta;

import java.util.Arrays;
import java.util.stream.Stream;

public class Document {
    private static int idCount = 0;

    private final int id;
    private final int locationId;
    private final int timeId;
    private final String[] terms;

    public Document(double longitude, double latitude, int timestamp, String[] terms) {
        this.id = idCount++;
        this.locationId = 0;
        this.timeId = 0;
        this.terms = terms;
    }

    public int getLocationId() {
        return locationId;
    }

    public int getTimestampId() {
        return timeId;
    }

    public Stream<String> getTerms() {
        return Arrays.stream(terms);
    }

    public boolean hasTimeAndLoc(int t, int l) {
        return t == timeId && l == locationId;
    }

    public int getId() {
        return id;
    }
}
