package de.progle.model;

import java.util.ArrayList;
import java.util.List;

public class CurriculumModule {

    private String id;
    private String titel;
    private String ueberblick;
    private List<String> voraussetzungen = new ArrayList<>();
    private List<String> lektionen = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getTitel() {
        return titel;
    }

    public String getUeberblick() {
        return ueberblick == null ? "" : ueberblick;
    }

    public List<String> getVoraussetzungen() {
        return voraussetzungen;
    }

    public List<String> getLektionen() {
        return lektionen;
    }
}
