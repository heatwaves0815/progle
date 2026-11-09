package de.progle.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class LessonData {

    private String id;

    @SerializedName("typ")
    private String typ;

    private String titel;

    private List<String> lernziele = new ArrayList<>();

    @SerializedName(value = "erklärung", alternate = {"erklaerung"})
    private String erklaerung;

    @SerializedName(value = "beispiel-code", alternate = {"beispielCode"})
    private String beispielCode;

    @SerializedName(value = "übungen", alternate = {"uebungen"})
    private List<Exercise> uebungen = new ArrayList<>();

    private List<QuizQuestion> quiz = new ArrayList<>();

    @SerializedName(value = "typische-fehler", alternate = {"typischeFehler"})
    private List<TypicalError> typischeFehler = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getTyp() {
        return typ == null ? "lektion" : typ;
    }

    public String getTitel() {
        return titel;
    }

    public List<String> getLernziele() {
        return lernziele;
    }

    public String getErklärung() {
        return erklaerung == null ? "" : erklaerung;
    }

    public String getBeispielCode() {
        return beispielCode == null ? "" : beispielCode;
    }

    public List<Exercise> getÜbungen() {
        return uebungen;
    }

    public List<QuizQuestion> getQuiz() {
        return quiz;
    }

    public List<TypicalError> getTypischeFehler() {
        return typischeFehler;
    }
}
