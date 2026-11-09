package de.progle.model;

public enum KnowledgeLevel {

    KEINE("Keine Vorkenntnisse"),
    ETWAS("Etwas Vorkenntnisse"),
    ERFAHREN("Erfahren in anderen Sprachen");

    private final String displayName;

    KnowledgeLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
