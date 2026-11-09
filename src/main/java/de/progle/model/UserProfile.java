package de.progle.model;

public class UserProfile {

    private String name;
    private KnowledgeLevel level;

    public UserProfile() {
    }

    public UserProfile(String name, KnowledgeLevel level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KnowledgeLevel getLevel() {
        return level == null ? KnowledgeLevel.KEINE : level;
    }

    public void setLevel(KnowledgeLevel level) {
        this.level = level;
    }
}
