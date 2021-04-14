package edu.ntnu.app.psta;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// TODO: Preprocessing? Extract vocabulary, locations
public class Documents {
    private final Document[] docs;
    private final Location[] locations;
    private final int[] timestamps;
    private final String[] vocabulary;
    private final Map<Integer, Long>[] wordDocCounts;
    public final double[] backgroundTheme;
    private int sumWordCounts;
    private int[] wordCounts;

    public Documents(String pathName) throws IOException {
        this.docs = readFile(pathName);


        this.wordDocCounts = new Map[docs.length];
        this.sumWordCounts = 0;
        this.wordCounts = new int[vocabulary.length];
        for (int i = 0; i < docs.length; i++) {
            this.wordDocCounts[i] = docs[i].getTerms().map(word -> Arrays.binarySearch(vocabulary, word)).collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            this.sumWordCounts += this.wordDocCounts[i].values().stream().reduce(0L, Long::sum);
            this.wordDocCounts[i].entrySet().stream().forEach(entry -> wordCounts[entry.getKey()] += entry.getValue());
        }

        this.backgroundTheme = new double[nWords()];
        for (int i = 0; i < nWords(); i++) {
            int wordCountInAllDocs = getWordCount(i);
            int sumAllWordCounts = getSumWordCount();
            this.backgroundTheme[i] = (double) wordCountInAllDocs / (double) sumAllWordCounts;
        }
    }

    // Read a file of format long \n lat \n city \n country code \n timestamp \n text \n long \n etc...
    // i.e. every 6 lines belong to one document. The method assumes there are exactly N*6 lines.
    private Document[] readFile(String pathName) throws IOException, NullPointerException {
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

    private Document createDocument(double lineLong, double lineLat, String lineCity, String lineCountry, int lineTimestamp, String[] lineText) {
        if (/*location exists*/) {

        } else {
            Location l = new Location(lineLat, lineLong, lineCity, lineCountry);
        }
        return null;
    }

//    public Documents(Document[] docs) {
//        this.locations = Arrays.stream(docs).map(d -> d.getLocationId()).distinct().toArray(Location[]::new);
//        this.timestamps = Arrays.stream(docs).mapToInt(d -> d.getTimestampId()).sorted().distinct().toArray();
//        this.vocabulary = Arrays.stream(docs).flatMap(d -> d.getTerms()).sorted().distinct().toArray(String[]::new);
//    }

    public int nTimeslots() {
        return timestamps.length;
    }

    public int nDocuments() {
        return docs.length;
    }

    public int nLocations() {
        return locations.length;
    }

    public int nWords() {
        return vocabulary.length;
    }

    public int getWordCount(int wordIndex) {
        return wordCounts[wordIndex];
    }

    public int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].getOrDefault(wordIndex, 0L).intValue();
    }

    public int getSumWordCount() {
        return sumWordCounts;
    }

    public Document get(int d) {
        return docs[d];
    }

    public IntStream getIndexOfDocsWithTL(int t, int l) {
        return Arrays.stream(docs).filter(d -> d.hasTimeAndLoc(t, l)).mapToInt(d -> d.getId());
    }
}
