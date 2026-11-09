package de.progle.model;

import com.google.gson.annotations.SerializedName;

public class Pruefung {

    private String art;

    private String erwartet;

    private String eingabe;

    public String getArt() {
        return art == null ? "ausgabe" : art;
    }

    public String getErwartet() {
        return erwartet == null ? "" : erwartet;
    }

    public String getEingabe() {
        return eingabe == null ? "" : eingabe;
    }

    public boolean hasEingabe() {
        return eingabe != null && !eingabe.isEmpty();
    }
}
