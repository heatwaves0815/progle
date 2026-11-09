package de.progle.model;

import com.google.gson.annotations.SerializedName;

public class TypicalError {

    private String fehler;

    @SerializedName(value = "erklärung", alternate = {"erklaerung"})
    private String erklaerung;

    public String getFehler() {
        return fehler;
    }

    public String getErklärung() {
        return erklaerung == null ? "" : erklaerung;
    }
}
