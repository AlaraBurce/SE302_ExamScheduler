package org.example.se302_examscheduler;

public class ExamSession {
    private final Course course;
    private final Classroom classroom;
    private final ExamSlot slot;

    public ExamSession(Course course, Classroom classroom, ExamSlot slot) {
        this.course = course;
        this.classroom = classroom;
        this.slot = slot;
    }

    public Course getCourse() {
        return course;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public ExamSlot getSlot() {
        return slot;
    }
}
