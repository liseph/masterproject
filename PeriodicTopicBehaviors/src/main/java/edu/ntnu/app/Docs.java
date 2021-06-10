package edu.ntnu.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class Docs {
    public static final double HOUR_MS = 3.6E+6;
    public static final double DAY_MS = 8.64E+7;
    public static final double TIME_CONVERT = DAY_MS;

    public static List<Document> docs;
    public static double docShare = 1.0;
    private static List<Location> locations;
    private static List<Long> timestamps;
    private static List<String> vocabulary;

    public static void setDocShare(double relDocShare) {
        docShare = relDocShare;
    }

    protected static void initialize(String pathName, int nDocs) throws IOException {
        // Init locations and vocabulary so we can build them as we read the input
        List<Location> locs = new ArrayList<>();
        Set<String> voc = new TreeSet<>();
        Set<Long> tss = new TreeSet<>();
        docs = readFile((int) (nDocs * docShare), pathName, locs, voc, tss);
        // Make locations, timestamps and vocabulary unmodifiable
        locations = Collections.unmodifiableList(locs);
        vocabulary = List.copyOf(voc);
        timestamps = List.copyOf(tss);
        System.out.format("#Locations: %d\n#Timestamps: %d\n#Words: %d\n", locations.size(), timestamps.size(), vocabulary.size());
    }

    // Format: long \n lat \n name \n city \n country code \n timestamp_ms \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
    // We also assume that the documents are sorted by timestamp.
    private static List<Document> readFile(int maxnDocs, String pathName, List<Location> locs, Set<String> voc, Set<Long> tss) throws IOException, NullPointerException {
        List<Document> data = new ArrayList<>();
        FileInputStream fileIn = new FileInputStream(pathName);
        GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
        BufferedReader in = new BufferedReader(new InputStreamReader(gzipIn));
        System.out.println("Scanning docs...");
        String firstLine;
        while ((firstLine = in.readLine()) != null && data.size() < maxnDocs) {
            long lineTimestamp = Long.parseLong(firstLine);
            Float lineLong = Float.parseFloat(in.readLine());
            Float lineLat = Float.parseFloat(in.readLine());
            String lineText = in.readLine();
            String lineName = in.readLine();
            String lineCity = in.readLine();
            String lineCountry = in.readLine();
            data.add(createDocument(locs, voc, tss, lineLong, lineLat, lineName, lineCity, lineCountry, lineTimestamp, lineText));
        }
        System.out.format("Successfully scanned %d docs\n", data.size());
        return data;
    }

    // Method to create a document from the input information. Creates a new location and time object if it does not exist, and adds its terms to the vocabulary.
    private static Document createDocument(List<Location> locs, Set<String> voc, Set<Long> tss, Float lineLong, Float lineLat, String lineName, String lineCity, String lineCountry, long lineTimestamp, String lineText) {
        // Append new location to locations list if it does not exist. If it does exist, simply store the index.
        Location l = new Location(lineLat, lineLong, lineName, lineCity, lineCountry);
        if (!locs.contains(l))
            locs.add(l); // Add at back of list, important to not change the indices.
        int locationIndex = locs.indexOf(l);

        // Add all the terms of the document to the vocabulary.
        voc.addAll(Arrays.asList(lineText.split(" ")));

        // Convert timestamp to days (or hours) since UNIX Epoch, store all timestamps found
        long ts = (long) (lineTimestamp / TIME_CONVERT);
        tss.add(ts);

        return new Document(locationIndex, ts, lineText);
    }

    public static Document getDoc(int d) {
        return docs.get(d);
    }

    public static int nTimeslots() {
        return timestamps.size();
    }

    public static int nDocuments() {
        return docs.size();
    }

    public static int nLocations() {
        return locations.size();
    }

    public static int nWords() {
        return vocabulary.size();
    }

    public static int getTimestampIndex(long timestamp) {
        Long[] ts = timestamps.toArray(Long[]::new);
        return IntStream.range(0, timestamps.size()).filter(i -> ts[i] == timestamp).findFirst().orElseThrow();
    }

    public static double getTimestamp(int t) {
        return timestamps.get(t);
    }

    public static List<String> getVocabulary() {
        return vocabulary;
    }

    public static String getWord(int w) {
        return vocabulary.get(w);
    }

    public static Location getLocation(int l) {
        return locations.get(l);
    }

    public static List<Location> getLocations() {
        return locations;
    }

    protected static void clear() {
        Document.reset();
        docs = null;
        locations = null;
        timestamps = null;
        vocabulary = null;
    }
}
