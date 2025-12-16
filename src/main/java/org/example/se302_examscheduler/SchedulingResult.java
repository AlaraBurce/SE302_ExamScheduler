package org.example.se302_examscheduler;

import java.util.List;

public class SchedulingResult {
    private final List<ExamSession> sessions;
    private final List<Course> unscheduledCourses;

    public SchedulingResult(List<ExamSession> sessions, List<Course> unscheduledCourses) {
        this.sessions = sessions;
        this.unscheduledCourses = unscheduledCourses;
    }

    public List<ExamSession> getSessions() {
        return sessions;
    }

    public List<Course> getUnscheduledCourses() {
        return unscheduledCourses;
    }
}
