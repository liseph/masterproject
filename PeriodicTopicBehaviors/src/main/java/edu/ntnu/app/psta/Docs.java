package edu.ntnu.app.psta;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Docs {
    public static Document[] docs;
    public static Float[] backgroundTheme;
    private static List<Location> locations;
    private static SortedSet<Long> timestamps; // TreeSet is sorted so we keep the order fixed
    private static SortedSet<String> vocabulary; // TreeSet is sorted so we keep the order fixed
    private static Map<Integer, MutableInt>[] wordDocCounts;
    private static int sumWordCounts;
    private static int[][] sumWordCountsTL;
    private static int[] wordCounts;
    private static Map<Integer, Map<Integer, List<Integer>>> indexOfDocsWithTL;
    private static Set<Integer>[] indexOfDocsWithWord;

    public static void initialize(String pathName) throws IOException {
        // Init locations and vocabulary so we can build them as we read the input
        locations = new ArrayList<>();
        vocabulary = new TreeSet<>();
        timestamps = new TreeSet<>();
        docs = readFile(pathName);
        // Make locations, timestamps and vocabulary unmodifiable
        locations = Collections.unmodifiableList(locations);
        vocabulary = Collections.unmodifiableSortedSet((TreeSet) vocabulary);
        timestamps = Collections.unmodifiableSortedSet((TreeSet) timestamps);
        // Calculate some statistics to use in calculations of topics
        wordDocCounts = new Map[docs.length];
        sumWordCounts = 0;
        sumWordCountsTL = new int[Docs.nTimeslots()][Docs.nLocations()];
        wordCounts = new int[vocabulary.size()];
        indexOfDocsWithWord = new Set[nWords()];
        indexOfDocsWithTL = new HashMap<>();
        System.out.println("Calculating word counts...");
        String[] voc = vocabulary.toArray(String[]::new);
        IntStream.range(0, docs.length).forEach(i -> {
            long t1 = System.nanoTime();
            Document doc = docs[i];
            int[] docTermIndices = doc.getTerms().mapToInt(word -> Arrays.binarySearch(voc, word)).toArray();
            long t2 = System.nanoTime() - t1;
            doc.setTermIndices(docTermIndices); // Store this as we need it for later
            int sumWordCount = docTermIndices.length;
            sumWordCounts += sumWordCount;
            sumWordCountsTL[doc.getTimestampId()][doc.getLocationId()] += sumWordCount;
            long t3 = System.nanoTime() - t2 - t1;
            wordDocCounts[i] = new HashMap<>();
            Arrays.stream(docTermIndices).forEach(index -> {
                wordCounts[index]++;
                indexOfDocsWithWord[index] = indexOfDocsWithWord[index] == null ? new HashSet<>() : indexOfDocsWithWord[index];
                indexOfDocsWithWord[index].add(i);
                MutableInt count = wordDocCounts[i].get(index);
                if (count == null) wordDocCounts[i].put(index, new MutableInt());
                else count.increment();
            });
            long t4 = System.nanoTime() - t3 - t2 - t1;
            // Calculating TL word indices, Map<t, Map<l, List<Index>>>
            int t = doc.getTimestampId();
            int l = doc.getLocationId();
            Map<Integer, List<Integer>> outer = indexOfDocsWithTL.get(t);
            if (outer == null) {
                outer = new HashMap<>();
                indexOfDocsWithTL.put(t, outer);
            }
            List<Integer> inner = outer.get(l);
            if (inner == null) {
                inner = new ArrayList<>();
                outer.put(l, inner);
            }
            inner.add(i);
            long t5 = System.nanoTime() - t4 - t3 - t2 - t1;
        });
        System.out.println("Calculating background topic...");
        backgroundTheme = new Float[nWords()];
        for (int i = 0; i < nWords(); i++) {
            int wordCountInAllDocs = getWordCount(i);
            int sumAllWordCounts = getSumWordCount();
            backgroundTheme[i] = wordCountInAllDocs * 1.0f / sumAllWordCounts;
        }

//        indexOfDocsWithTL = new HashMap<>();
//        IntStream.range(0, nTimeslots()).forEach(t -> {
//            indexOfDocsWithTL.add(new ArrayList<>());
//            for (int l = 0; l < nLocations(); l++) {
//                int finalT = t;
//                int finalL = l;
//                indexOfDocsWithTL.get(t).add(Arrays.stream(docs).filter(d -> d.hasTimeAndLoc(finalT, finalL)).mapToInt(d -> d.getId()).toArray());
//            }
//        });
    }

    // Format: long \n lat \n name \n city \n country code \n timestamp_ms \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
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
                String[] lineText = sc.nextLine().split(" ");
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
    private static Document createDocument(Float lineLong, Float lineLat, String lineName, String lineCity, String lineCountry, long lineTimestamp, String[] lineText) {
        // Append new location to locations list if it does not exist. If it does exist, simply store the index.
        Location l = new Location(lineLat, lineLong, lineName, lineCity, lineCountry);
        if (!locations.contains(l))
            locations.add(l); // Add at back of list, important to not change the indices.
        int locationIndex = locations.indexOf(l);

        // Add all the terms of the document to the vocabulary.
        vocabulary.addAll(Arrays.asList(lineText));

        // Convert timestamp to days (or hours) since UNIX Epoch, store all timestamps found
        long ts = (long) (lineTimestamp / PSTA.TIME_CONVERT);
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

    public static int getWordCount(int wordIndex) {
        return wordCounts[wordIndex];
    }

    public static int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].get(wordIndex) == null ? 0 : wordDocCounts[docIndex].get(wordIndex).get();
    }

    public static int getSumWordCount() {
        return sumWordCounts;
    }

    public static int getSumWordCount(int t, int l) {
        return sumWordCountsTL[t][l];
    }

    public static Stream<Integer> getIndexOfDocsWithTL(int t, int l) {
        return indexOfDocsWithTL.getOrDefault(t, new HashMap<>()).getOrDefault(l, new ArrayList<>()).stream();
    }

    public static IntStream getIndexOfDocsWithWord(int w) {
        return indexOfDocsWithWord[w].stream().mapToInt(Integer::intValue);
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

    static class MutableInt {
        int value = 1; // note that we start at 1 since we're counting

        public void increment() {
            ++value;
        }

        public int get() {
            return value;
        }
    }
}
