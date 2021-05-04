package edu.ntnu.app.psta;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PstaDocs extends Docs {

    public static double[] backgroundTheme;
    private static Map<Integer, MutableInt>[] wordDocCounts;
    private static int sumWordCounts;
    private static int[][] sumWordCountsTL;
    private static int[] wordCounts;
    private static Map<Integer, Map<Integer, List<Integer>>> indexOfDocsWithTL;
    private static Set<Integer>[] indexOfDocsWithWord;


    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
        // Calculate some statistics to use in calculations of topics
        wordDocCounts = new Map[nDocuments()];
        sumWordCounts = 0;
        sumWordCountsTL = new int[Docs.nTimeslots()][Docs.nLocations()];
        wordCounts = new int[nWords()];
        indexOfDocsWithWord = new Set[nWords()];
        indexOfDocsWithTL = new HashMap<>();
        System.out.println("Calculating word counts...");
        Object[] voc = getVocabulary().toArray();
        IntStream.range(0, nDocuments()).forEach(i -> {
            Document doc = getDoc(i);

            int[] docTermIndices = Arrays.stream(doc.getTerms().split(" ")).mapToInt(word -> Arrays.binarySearch(voc, word)).toArray();
            doc.setTermIndices(docTermIndices); // Store this as we need it for later
            int sumWordCount = docTermIndices.length;
            sumWordCounts += sumWordCount;
            sumWordCountsTL[doc.getTimestampId()][doc.getLocationId()] += sumWordCount;
            wordDocCounts[i] = new HashMap<>();
            Arrays.stream(docTermIndices).forEach(index -> {
                wordCounts[index]++;
                indexOfDocsWithWord[index] = indexOfDocsWithWord[index] == null ? new HashSet<>() : indexOfDocsWithWord[index];
                indexOfDocsWithWord[index].add(i);
                MutableInt count = wordDocCounts[i].get(index);
                if (count == null) wordDocCounts[i].put(index, new MutableInt());
                else count.increment();
            });
            // Calculating TL word indices, Map<t, Map<l, List<Index>>>
            int t = doc.getTimestampId();
            int l = doc.getLocationId();
            Map<Integer, List<Integer>> outer = indexOfDocsWithTL.computeIfAbsent(t, k -> new HashMap<>());
            List<Integer> inner = outer.computeIfAbsent(l, k -> new ArrayList<>());
            inner.add(i);
        });
        System.out.println("Calculating background topic...");
        backgroundTheme = new double[nWords()];
        for (int i = 0; i < nWords(); i++) {
            int wordCountInAllDocs = getWordCount(i);
            int sumAllWordCounts = getSumWordCount();
            backgroundTheme[i] = wordCountInAllDocs * 1.0 / sumAllWordCounts;
        }
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
