package de.progle.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {

    private String frage;

    private List<String> optionen = new ArrayList<>();

    private int richtig;

    @SerializedName(value = "erklärung", alternate = {"erklaerung"})
    private String erklaerung;

    public String getFrage() {
        return frage;
    }

    public List<String> getOptionen() {
        return optionen;
    }

    public int getRichtig() {
        return richtig;
    }

    public String getErklärung() {
        return erklaerung == null ? "" : erklaerung;
    }
}
