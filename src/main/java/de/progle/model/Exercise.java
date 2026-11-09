package de.progle.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Exercise {

    private String aufgabenstellung;

    @SerializedName(value = "start-code", alternate = {"startCode"})
    private String startCode;

    private List<String> hinweise = new ArrayList<>();

    @SerializedName(value = "lösung", alternate = {"loesung"})
    private String lösung;

    @SerializedName(value = "prüfung", alternate = {"pruefung"})
    private Pruefung prüfung;

    public String getAufgabenstellung() {
        return aufgabenstellung;
    }

    public String getStartCode() {
        return startCode == null ? "" : startCode;
    }

    public List<String> getHinweise() {
        return hinweise;
    }

    public String getLösung() {
        return lösung;
    }

    public Pruefung getPrüfung() {
        return prüfung;
    }
}
