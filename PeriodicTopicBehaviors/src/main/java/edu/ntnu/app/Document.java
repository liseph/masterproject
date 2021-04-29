package edu.ntnu.app;

public class Document {
    private static int idCount = 0;

    private final int id;
    private final int locationId;
    private final long timestamp;
    private final String terms;
    private int timestampIndex = -1;
    private int[] termIndices;

    public Document(int locationId, long timestamp, String terms) {
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

    public String getTerms() {
        return terms;
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

    public long getTimestamp() {
        return timestamp;
    }
}
