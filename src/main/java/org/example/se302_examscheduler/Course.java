package org.example.se302_examscheduler;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private final String code;
    private final List<Student> students = new ArrayList<>();
    private ExamSession examSession;

    public Course(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void addStudent(Student s) {
        if (!students.contains(s)) {
            students.add(s);
        }
    }

    public ExamSession getExamSession() {
        return examSession;
    }

    public void setExamSession(ExamSession examSession) {
        this.examSession = examSession;
    }

    @Override
    public String toString() {
        return code;
    }
}