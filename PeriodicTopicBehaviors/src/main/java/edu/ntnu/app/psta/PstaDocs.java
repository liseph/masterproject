package edu.ntnu.app.psta;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PstaDocs extends Docs {

    public static Float[] backgroundTheme;
    private static Map<Integer, MutableInt>[] wordDocCounts;
    private static int sumWordCounts;
    private static int[][] sumWordCountsTL;
    private static int[] wordCounts;
    private static Map<Integer, Map<Integer, List<Integer>>> indexOfDocsWithTL;
    private static Set<Integer>[] indexOfDocsWithWord;


    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
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
