package edu.ntnu.app.psta;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Docs {
    public static Document[] docs;
    public static double[] backgroundTheme;

    private static List<Location> locations;
    private static long timestampMinValue = Long.MAX_VALUE;
    private static long timestampMaxValue = 0;
    private static Set<String> vocabulary; // TreeSet is sorted so we keep the order fixed
    private static Map<Integer, Long>[] wordDocCounts;
    private static int sumWordCounts;
    private static int[] wordCounts;
    private static List<List<int[]>> indexOfDocsWithTL;
    private static Set<Integer>[] indexOfDocsWithWord;

    public static void initialize(String pathName) throws IOException {
        // Init locations and vocabulary so we can build them as we read the input
        locations = new ArrayList<>();
        vocabulary = new TreeSet<>();
        docs = readFile(pathName);
        // Make locations and vocabulary unmodifiable
        locations = Collections.unmodifiableList(locations);
        vocabulary = Collections.unmodifiableSortedSet((TreeSet)vocabulary);
        // Calculate some statistics to use in calculations of topics
        wordDocCounts = new Map[docs.length];
        sumWordCounts = 0;
        wordCounts = new int[vocabulary.size()];
        indexOfDocsWithWord = new Set[nWords()];
        for (int i = 0; i < docs.length; i++) {
            Integer[] docTermIndices = docs[i].getTerms().map(word -> Arrays.binarySearch(vocabulary.toArray(), word)).toArray(Integer[]::new);
            docs[i].setTermIndices(docTermIndices); // Store this as we need it for later
            wordDocCounts[i] = Arrays.stream(docTermIndices).collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            sumWordCounts += wordDocCounts[i].values().stream().reduce(0L, Long::sum);
            wordDocCounts[i].entrySet().stream().forEach(entry -> wordCounts[entry.getKey()] += entry.getValue());

            int finalI = i;
            Arrays.stream(docTermIndices).forEach(index -> {
                if (indexOfDocsWithWord[index] == null) {
                    indexOfDocsWithWord[index] = new HashSet<>();
                }
                indexOfDocsWithWord[index].add(finalI);
            });
        }

        backgroundTheme = new double[nWords()];
        for (int i = 0; i < nWords(); i++) {
            int wordCountInAllDocs = getWordCount(i);
            int sumAllWordCounts = getSumWordCount();
            backgroundTheme[i] = (double) wordCountInAllDocs / (double) sumAllWordCounts;
        }

        indexOfDocsWithTL = new ArrayList<>();
        for (int t = 0; t < nTimeslots(); t++) {
            indexOfDocsWithTL.add(new ArrayList<>());
            for (int l = 0; l < nLocations(); l++) {
                int finalT = t;
                int finalL = l;
                indexOfDocsWithTL.get(t).add(Arrays.stream(docs).filter(d -> d.hasTimeAndLoc(finalT, finalL)).mapToInt(d -> d.getId()).toArray());
            }
        }
    }

    // Format: long \n lat \n name \n city \n country code \n timestamp_ms \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
    private static Document[] readFile(String pathName) throws IOException, NullPointerException {
        List<Document> data = new ArrayList<>();
        FileInputStream inputStream = null;
        Scanner sc = null;
        try {
            inputStream = new FileInputStream(pathName);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                long lineTimestamp = Long.parseLong(sc.nextLine());
                double lineLong = Double.parseDouble(sc.nextLine());
                double lineLat = Double.parseDouble(sc.nextLine());
                String[] lineText = sc.nextLine().split(" ");
                String lineName = sc.nextLine();
                String lineCity = sc.nextLine();
                String lineCountry = sc.nextLine();
                data.add(createDocument(lineLong, lineLat, lineName, lineCity, lineCountry, lineTimestamp, lineText));
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
    private static Document createDocument(double lineLong, double lineLat, String lineName, String lineCity, String lineCountry, long lineTimestamp, String[] lineText) {
        // Append new location to locations list if it does not exist. If it does exist, simply store the index.
        Location l = new Location(lineLat, lineLong, lineName, lineCity, lineCountry);
        if (!locations.contains(l))
            locations.add(l); // Add at back of list, important to not change the indices.
        int locationIndex = locations.indexOf(l);

        // Add all the terms of the document to the vocabulary.
        vocabulary.addAll(Arrays.asList(lineText));

        // Convert timestamp to days (or hours) since UNIX Epoch, store max and min timestamp value found
        long ts = (long) (lineTimestamp/PSTA.TIME_CONVERT);
        if (ts > timestampMaxValue) {
            timestampMaxValue = ts;
        } else if (ts < timestampMinValue) {
            timestampMinValue = ts;
        }

        return new Document(locationIndex, ts, lineText);
    }

    public static Document get(int d) {
        return docs[d];
    }

    public static int nTimeslots() {
        return Math.toIntExact(timestampMaxValue + 1 - timestampMinValue);
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

    public static int getWordCount(int wordIndex) {
        return wordCounts[wordIndex];
    }

    public static int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].getOrDefault(wordIndex, 0L).intValue();
    }

    public static int getSumWordCount() {
        return sumWordCounts;
    }

    public static IntStream getIndexOfDocsWithTL(int t, int l) {
        return Arrays.stream(indexOfDocsWithTL.get(t).get(l));
    }

    public static IntStream getIndexOfDocsWithWord(int w) {
        return indexOfDocsWithWord[w].stream().mapToInt(Integer::intValue);
    }

    public static int getTimestampId(long timestamp) {
        return Math.toIntExact(timestamp - timestampMinValue);
    }

    public static String[] getVocabulary() {
        return vocabulary.toArray(String[]::new);
    }
}
