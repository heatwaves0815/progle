package de.progle.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Curriculum {

    private String id;
    private String sprache;
    private String titel;
    private String beschreibung;
    private List<String> schwierigkeitsstufen = new ArrayList<>();
    private List<CurriculumModule> module = new ArrayList<>();
    private transient Map<String, LessonData> lessonIndex = new java.util.HashMap<>();

    public String getId() {
        return id;
    }

    public String getSprache() {
        return sprache == null ? titel : sprache;
    }

    public String getTitel() {
        return titel;
    }

    public String getBeschreibung() {
        return beschreibung == null ? "" : beschreibung;
    }

    public List<String> getSchwierigkeitsstufen() {
        return schwierigkeitsstufen;
    }

    public List<CurriculumModule> getModule() {
        return module;
    }

    public LessonData lessonById(String lessonId) {
        return lessonIndex.get(lessonId);
    }

    public void setLessonIndex(Map<String, LessonData> index) {
        this.lessonIndex = index;
    }
}
