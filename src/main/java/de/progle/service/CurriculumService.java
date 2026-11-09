package de.progle.service;

import com.google.gson.Gson;
import de.progle.model.Curriculum;
import de.progle.model.LessonData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CurriculumService {

    public record CurriculumInfo(String id, String titel, String beschreibung) {
    }

    private static final Gson GSON = new Gson();
    private final Path baseDir;

    public CurriculumService() {
        this(AppDirs.resolve("curricula"));
    }

    public CurriculumService(Path baseDir) {
        this.baseDir = baseDir;
    }

    public List<CurriculumInfo> listAvailable() {
        List<CurriculumInfo> infos = new ArrayList<>();
        if (!Files.isDirectory(baseDir)) {
            return infos;
        }
        try (Stream<Path> dirs = Files.list(baseDir)) {
            dirs.filter(Files::isDirectory).sorted().forEach(dir -> {
                Path file = dir.resolve("curriculum.json");
                if (Files.isRegularFile(file)) {
                    try {
                        Curriculum curriculum = GSON.fromJson(
                                Files.readString(file, StandardCharsets.UTF_8), Curriculum.class);
                        if (curriculum != null && curriculum.getId() != null) {
                            infos.add(new CurriculumInfo(curriculum.getId(), curriculum.getTitel(),
                                    curriculum.getBeschreibung()));
                        }
                    } catch (Exception e) {
                        System.err.println("curriculum.json unlesbar in " + dir + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("curricula-Ordner nicht lesbar: " + e.getMessage());
        }
        return infos;
    }

    public Curriculum load(String curriculumId) throws IOException {
        Path file = baseDir.resolve(curriculumId).resolve("curriculum.json");
        Curriculum curriculum = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Curriculum.class);
        if (curriculum == null || curriculum.getId() == null) {
            throw new IOException("curriculum.json ist leer oder ungültig: " + file);
        }
        Map<String, LessonData> index = new LinkedHashMap<>();
        loadDir(baseDir.resolve(curriculumId).resolve("lessons"), index);
        loadDir(baseDir.resolve(curriculumId).resolve("projects"), index);
        curriculum.setLessonIndex(index);

        int fehlend = 0;
        for (var module : curriculum.getModule()) {
            for (String lessonId : module.getLektionen()) {
                if (!index.containsKey(lessonId)) {
                    System.err.println("WARNUNG: Lektion fehlt: " + lessonId);
                    fehlend++;
                }
            }
        }
        if (fehlend > 0) {
            System.err.println("WARNUNG: " + fehlend + " Lektionen fehlen im Curriculum '" + curriculumId + "'.");
        }
        return curriculum;
    }

    private void loadDir(Path dir, Map<String, LessonData> index) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json")).sorted().forEach(p -> {
                try {
                    LessonData lesson = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), LessonData.class);
                    if (lesson != null && lesson.getId() != null) {
                        index.put(lesson.getId(), lesson);
                    }
                } catch (Exception e) {
                    System.err.println("Lektionsdatei unlesbar: " + p + " -> " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Ordner nicht lesbar: " + dir + " -> " + e.getMessage());
        }
    }
}
