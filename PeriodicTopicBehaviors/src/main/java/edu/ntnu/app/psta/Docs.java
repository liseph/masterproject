package edu.ntnu.app.psta;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: Preprocessing? Extract vocabulary, locations
public static class Documents {
    private static Document[] docs;
    private static List<Location> locations;
    private static Set<Integer> timestamps;
    private static Set<String> vocabulary; // TreeSet is sorted so we keep the order fixed
    private static Map<Integer, Long>[] wordDocCounts;
    public static double[] backgroundTheme;
    private static int sumWordCounts;
    private static int[] wordCounts;

    public static void initialize(String pathName) throws IOException {
        // Init locations, vocabulary and timestamps so we can build them as we read the input
        locations = new ArrayList<>();
        vocabulary = new TreeSet<>();
        timestamps = new TreeSet<>();

        docs = readFile(pathName);
        // Make locations, vocabulary and timestamps unmodifiable
        locations = Collections.unmodifiableList(locations);
        vocabulary = Collections.unmodifiableSortedSet((TreeSet)vocabulary);


        wordDocCounts = new Map[docs.length];
        sumWordCounts = 0;
        wordCounts = new int[vocabulary.size()];
        for (int i = 0; i < docs.length; i++) {
            wordDocCounts[i] = docs[i].getTerms().map(word -> Arrays.binarySearch(vocabulary.toArray(), word)).collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            sumWordCounts += wordDocCounts[i].values().stream().reduce(0L, Long::sum);
            wordDocCounts[i].entrySet().stream().forEach(entry -> wordCounts[entry.getKey()] += entry.getValue());
        }

        backgroundTheme = new double[nWords()];
        for (int i = 0; i < nWords(); i++) {
            int wordCountInAllDocs = getWordCount(i);
            int sumAllWordCounts = getSumWordCount();
            backgroundTheme[i] = (double) wordCountInAllDocs / (double) sumAllWordCounts;
        }
    }

    // Read a file of format long \n lat \n city \n country code \n timestamp \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
    private static Document[] readFile(String pathName) throws IOException, NullPointerException {
        List<Document> data = new ArrayList<>();
        FileInputStream inputStream = null;
        Scanner sc = null;
        try {
            inputStream = new FileInputStream(pathName);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                double lineLong = Double.parseDouble(sc.nextLine());
                double lineLat = Double.parseDouble(sc.nextLine());
                String lineCity = sc.nextLine();
                String lineCountry = sc.nextLine();
                int lineTimestamp = Integer.parseInt(sc.nextLine());
                String[] lineText = sc.nextLine().split(" ");
                data.add(createDocument(lineLong, lineLat, lineCity, lineCountry, lineTimestamp, lineText));
            }
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
    private static Document createDocument(double lineLong, double lineLat, String lineCity, String lineCountry, int lineTimestamp, String[] lineText) {
        // Append new location to locations list if it does not exist. If it does exist, simply store the index.
        Location l = new Location(lineLat, lineLong, lineCity, lineCountry);
        if (!locations.contains(l))
            locations.add(l); // Add at back of list, important to not change the indices.
        int locationIndex = locations.indexOf(l);

        // Add all the terms of the document to the vocabulary.
        vocabulary.addAll(Arrays.asList(lineText));

        // Add timestamp to list of timestamps
        timestamps.add(lineTimestamp);

        return new Document(locationIndex, lineTimestamp, lineText);
    }

//    public Documents(Document[] docs) {
//        locations = Arrays.stream(docs).map(d -> d.getLocationId()).distinct().toArray(Location[]::new);
//        timestamps = Arrays.stream(docs).mapToInt(d -> d.getTimestampId()).sorted().distinct().toArray();
//        vocabulary = Arrays.stream(docs).flatMap(d -> d.getTerms()).sorted().distinct().toArray(String[]::new);
//    }

    public static int nTimeslots() {
        return timestamps.length;
    }

    public static int nDocuments() {
        return docs.length;
    }

    public static int nLocations() {
        return locations.length;
    }

    public static int nWords() {
        return vocabulary.length;
    }

    public static int getWordCount(int wordIndex) {
        return wordCounts[wordIndex];
    }

    public static int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].getOrDefault(wordIndex, 0L).intValue();
    }

    public static int getSumWordCount() {
        return sumWordCounts;
    }

    public static Document get(int d) {
        return docs[d];
    }

    public static IntStream getIndexOfDocsWithTL(int t, int l) {
        return Arrays.stream(docs).filter(d -> d.hasTimeAndLoc(t, l)).mapToInt(d -> d.getId());
    }
}
