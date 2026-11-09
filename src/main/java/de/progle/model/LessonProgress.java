package de.progle.model;

import java.util.ArrayList;
import java.util.List;

public class LessonProgress {

    private String lessonId;
    private boolean completed;
    private List<Boolean> uebungFertig = new ArrayList<>();
    private List<String> codes = new ArrayList<>();
    private List<Integer> hintStufen = new ArrayList<>();
    private List<Boolean> quizRichtig = new ArrayList<>();

    public LessonProgress() {
    }

    public LessonProgress(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void ensureSize(int übungsAnzahl, int quizAnzahl) {
        while (uebungFertig.size() < übungsAnzahl) {
            uebungFertig.add(false);
        }
        while (codes.size() < übungsAnzahl) {
            codes.add("");
        }
        while (hintStufen.size() < übungsAnzahl) {
            hintStufen.add(0);
        }
        while (quizRichtig.size() < quizAnzahl) {
            quizRichtig.add(false);
        }
    }

    public boolean isÜbungFertig(int index) {
        return index >= 0 && index < uebungFertig.size() && uebungFertig.get(index);
    }

    public void setÜbungFertig(int index, boolean fertig) {
        while (uebungFertig.size() <= index) {
            uebungFertig.add(false);
        }
        uebungFertig.set(index, fertig);
    }

    public String getCode(int index) {
        return index >= 0 && index < codes.size() ? codes.get(index) : "";
    }

    public void setCode(int index, String code) {
        while (codes.size() <= index) {
            codes.add("");
        }
        codes.set(index, code);
    }

    public int getHintStufe(int index) {
        return index >= 0 && index < hintStufen.size() ? hintStufen.get(index) : 0;
    }

    public void setHintStufe(int index, int stufe) {
        while (hintStufen.size() <= index) {
            hintStufen.add(0);
        }
        hintStufen.set(index, stufe);
    }

    public boolean isQuizRichtig(int index) {
        return index >= 0 && index < quizRichtig.size() && quizRichtig.get(index);
    }

    public void setQuizRichtig(int index, boolean richtig) {
        while (quizRichtig.size() <= index) {
            quizRichtig.add(false);
        }
        quizRichtig.set(index, richtig);
    }

    public boolean isAllQuizRichtig() {
        for (Boolean ok : quizRichtig) {
            if (ok == null || !ok) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllÜbungenFertig() {
        for (Boolean ok : uebungFertig) {
            if (ok == null || !ok) {
                return false;
            }
        }
        return true;
    }
}
