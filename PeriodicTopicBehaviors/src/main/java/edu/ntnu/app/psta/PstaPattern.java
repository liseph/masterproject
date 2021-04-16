package edu.ntnu.app.psta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PstaPattern {
    private final VariableList topicDistTLs;
    private final VariableList topicDistDocs;
    private final VariableList themes;

    public PstaPattern(VariableList themes, VariableList topicDistDocs, VariableList topicDistTLs) {
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
            pw = new PrintWriter(new FileWriter("../pstaPatterns.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        pw.write("THEMES:");
        pw.write(themes.toString());
        pw.write("\nTOPIC DISTRIBUTION PER DOCUMENT:");
        pw.write(topicDistDocs.toString());
        pw.write("\nTOPIC DISTRIBUTION PER TIME,LOCATION PAIR:");
        pw.write(topicDistTLs.toString());

        pw.close();
    }
}
