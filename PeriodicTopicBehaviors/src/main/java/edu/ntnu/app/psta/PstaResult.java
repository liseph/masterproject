package edu.ntnu.app.psta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PstaResult {
    private final VariableList topicDistTLs;
    private final VariableList topicDistDocs;
    private final VariableList themes;

    public PstaResult(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs) {
        this.themes = themes;
        this.topicDistDocs = topicDistDocs;
        this.topicDistTLs = topicDistTLs;
    }

    public VariableList getTopicDistTLs() {
        return topicDistTLs;
    }

    public void writeToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("../pstaPatterns1.txt"))) {
            pw.write("THEMES:");
            pw.write(themes.toStringForFile());
            pw.write("\nTOPIC DISTRIBUTION PER DOCUMENT:");
            pw.write(topicDistDocs.toStringForFile());
            pw.write("\nTOPIC DISTRIBUTION PER TIME,LOCATION PAIR:");
            pw.write(topicDistTLs.toStringForFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int nTopics() {
        return themes.length();
    }
}
