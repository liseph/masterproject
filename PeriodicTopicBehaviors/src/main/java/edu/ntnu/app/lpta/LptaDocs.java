package edu.ntnu.app.lpta;

import edu.ntnu.app.Docs;
import edu.ntnu.app.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class LptaDocs extends Docs {

    private static Map<Integer, MutableInt>[] wordDocCounts;
    private static Set<Integer>[] indexOfDocsWithWord;

    public static void initialize(String pathName) throws IOException {
        Docs.initialize(pathName);
        // Calculate word counts
        Object[] voc = getVocabulary().toArray();
        wordDocCounts = new HashMap[nDocuments()];
        indexOfDocsWithWord = new Set[nWords()];
        IntStream.range(0, nDocuments()).forEach(i -> {
            Document doc = getDoc(i);
            int[] docTermIndices = Arrays.stream(doc.getTerms().split(" ")).mapToInt(word -> Arrays.binarySearch(voc, word)).toArray();
            doc.setTermIndices(docTermIndices);
            wordDocCounts[i] = new HashMap<>();
            Arrays.stream(docTermIndices).forEach(index -> {
                indexOfDocsWithWord[index] = indexOfDocsWithWord[index] == null ? new HashSet<>() : indexOfDocsWithWord[index];
                indexOfDocsWithWord[index].add(i);
                MutableInt count = wordDocCounts[i].get(index);
                if (count == null) wordDocCounts[i].put(index, new MutableInt());
                else count.increment();
            });
        });
    }

    public static int getWordCount(int docIndex, int wordIndex) {
        return wordDocCounts[docIndex].get(wordIndex) == null ? 0 : wordDocCounts[docIndex].get(wordIndex).get();
    }

    public static int[] getDocsInLoc(int l) {
        return docs.stream().filter(d -> d.getLocationId() == l).mapToInt(Document::getId).toArray();
    }

    public static IntStream getDocsWithWord(int w) {
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
