package edu.ntnu.app.psta;

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
}
