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

    public VariableList getTopicDistDocs() {
        return topicDistDocs;
    }

    public VariableList getThemes() {
        return themes;
    }

    public void writeToFile() {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter("../pstaPatterns1.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        pw.write("THEMES:");
        pw.write(themes.toStringForFile());
        pw.write("\nTOPIC DISTRIBUTION PER DOCUMENT:");
        pw.write(topicDistDocs.toStringForFile());
        pw.write("\nTOPIC DISTRIBUTION PER TIME,LOCATION PAIR:");
        pw.write(topicDistTLs.toStringForFile());

        pw.close();
    }

    public int nTopics() {
        return themes.length();
    }
}
