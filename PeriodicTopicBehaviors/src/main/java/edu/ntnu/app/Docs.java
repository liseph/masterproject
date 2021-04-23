package edu.ntnu.app;

import edu.ntnu.app.psta.Psta;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class Docs {
    public static Document[] docs;
    protected static List<Location> locations;
    protected static SortedSet<Long> timestamps; // TreeSet is sorted so we keep the order fixed
    protected static SortedSet<String> vocabulary; // TreeSet is sorted so we keep the order fixed

    protected static void initialize(String pathName) throws IOException {
        // Init locations and vocabulary so we can build them as we read the input
        locations = new ArrayList<>();
        vocabulary = new TreeSet<>();
        timestamps = new TreeSet<>();
        docs = readFile(pathName);
        // Make locations, timestamps and vocabulary unmodifiable
        locations = Collections.unmodifiableList(locations);
        vocabulary = Collections.unmodifiableSortedSet((TreeSet) vocabulary);
        timestamps = Collections.unmodifiableSortedSet((TreeSet) timestamps);
    }

    // Format: long \n lat \n name \n city \n country code \n timestamp_ms \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
    // We also assume that the documents are sorted by timestamp.
    private static Document[] readFile(String pathName) throws IOException, NullPointerException {
        List<Document> data = new ArrayList<>();
        FileInputStream inputStream = null;
        Scanner sc = null;
        try {
            inputStream = new FileInputStream(pathName);
            sc = new Scanner(inputStream, StandardCharsets.UTF_8);
            System.out.println("Scanning docs...");
            while (sc.hasNextLine()) {
                long lineTimestamp = Long.parseLong(sc.nextLine());
                Float lineLong = Float.parseFloat(sc.nextLine());
                Float lineLat = Float.parseFloat(sc.nextLine());
                String lineText = sc.nextLine();
                String lineName = sc.nextLine();
                String lineCity = sc.nextLine();
                String lineCountry = sc.nextLine();
                data.add(createDocument(lineLong, lineLat, lineName, lineCity, lineCountry, lineTimestamp, lineText));
            }
            System.out.format("Successfully scanned all %d docs...\n", data.size());
            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        return data.toArray(Document[]::new);
    }

    // Method to create a document from the input information. Creates a new location and time object if it does not exist, and adds its terms to the vocabulary.
    private static Document createDocument(Float lineLong, Float lineLat, String lineName, String lineCity, String lineCountry, long lineTimestamp, String lineText) {
        // Append new location to locations list if it does not exist. If it does exist, simply store the index.
        Location l = new Location(lineLat, lineLong, lineName, lineCity, lineCountry);
        if (!locations.contains(l))
            locations.add(l); // Add at back of list, important to not change the indices.
        int locationIndex = locations.indexOf(l);

        // Add all the terms of the document to the vocabulary.
        vocabulary.addAll(Arrays.asList(lineText));

        // Convert timestamp to days (or hours) since UNIX Epoch, store all timestamps found
        long ts = (long) (lineTimestamp / Psta.TIME_CONVERT);
        timestamps.add(ts);

        return new Document(locationIndex, ts, lineText);
    }

    public static Document get(int d) {
        return docs[d];
    }

    public static int nTimeslots() {
        return timestamps.size();
    }

    public static int nDocuments() {
        return docs.length;
    }

    public static int nLocations() {
        return locations.size();
    }

    public static int nWords() {
        return vocabulary.size();
    }

    public static int getTimestampIndex(long timestamp) {
        Long[] ts = timestamps.toArray(Long[]::new);
        return IntStream.range(0, timestamps.size()).filter(i -> ts[i] == timestamp).findFirst().getAsInt();
    }

    public static String[] getVocabulary() {
        return vocabulary.toArray(String[]::new);
    }

    public static Location[] getLocations() {
        return locations.toArray(Location[]::new);
    }
}
