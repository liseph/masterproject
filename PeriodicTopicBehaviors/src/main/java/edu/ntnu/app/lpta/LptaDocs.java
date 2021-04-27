package edu.ntnu.app.lpta;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class LptaDocs extends Docs {

    private static Map<Integer, MutableInt>[] wordDocCounts;

    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
        // Calculate word counts
        String[] voc = vocabulary.toArray(String[]::new);
        wordDocCounts = new HashMap[docs.length];
        IntStream.range(0, docs.length).forEach(i -> {
            Document doc = docs[i];
            int[] docTermIndices = Arrays.stream(doc.getTerms().split(" ")).mapToInt(word -> Arrays.binarySearch(voc, word)).toArray();
            wordDocCounts[i] = new HashMap<>();
            Arrays.stream(docTermIndices).forEach(index -> {
                MutableInt count = wordDocCounts[i].get(index);
                if (count == null) wordDocCounts[i].put(index, new MutableInt());
                else count.increment();
            });
        });
        System.out.println("hei");
    }

    public static int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].get(wordIndex) == null ? 0 : wordDocCounts[docIndex].get(wordIndex).get();
    }

    public static int[] getDocsInLoc(int l) {
        return Arrays.stream(docs).filter(d -> d.getLocationId() == l).mapToInt(d -> d.getId()).toArray();
    }

    public static long getTimestamp(int t) {
        return timestamps.toArray(Long[]::new)[t];
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
