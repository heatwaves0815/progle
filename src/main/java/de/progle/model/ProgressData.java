package de.progle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgressData {

    private String curriculumId;
    private UserProfile profile;
    private List<LessonProgress> lessons = new ArrayList<>();
    private int currentLessonIndex;

    public ProgressData() {
    }

    public ProgressData(String curriculumId, UserProfile profile, List<LessonData> orderedLessons) {
        this.curriculumId = curriculumId;
        this.profile = profile;
        for (LessonData lesson : orderedLessons) {
            LessonProgress progress = new LessonProgress(lesson.getId());
            progress.ensureSize(lesson.getÜbungen().size(), lesson.getQuiz().size());
            lessons.add(progress);
        }
    }

    public String getCurriculumId() {
        return curriculumId;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<LessonProgress> getLessons() {
        return lessons;
    }

    public LessonProgress progressFor(String lessonId) {
        for (LessonProgress progress : lessons) {
            if (progress.getLessonId() != null && progress.getLessonId().equals(lessonId)) {
                return progress;
            }
        }
        LessonProgress progress = new LessonProgress(lessonId);
        lessons.add(progress);
        return progress;
    }

    public int getCurrentLessonIndex() {
        return currentLessonIndex;
    }

    public void setCurrentLessonIndex(int index) {
        this.currentLessonIndex = index;
    }

    public long completedCount() {
        return lessons.stream().filter(LessonProgress::isCompleted).count();
    }

    public Optional<LessonProgress> nextAfter(String lessonId) {
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getLessonId() != null && lessons.get(i).getLessonId().equals(lessonId)) {
                if (i + 1 < lessons.size()) {
                    return Optional.of(lessons.get(i + 1));
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
