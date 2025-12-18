package org.example.se302_examscheduler;

import org.example.se302_examscheduler.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedule {
    private final List<Course> courses = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<Classroom> classrooms = new ArrayList<>();
    private final List<ExamSlot> examSlots = new ArrayList<>();
    private final List<ExamSession> examSessions = new ArrayList<>();

    public List<Course> getCourses() {
        return courses;
    }

    public List<Student> getStudents() {
        return students;
    }

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    public List<ExamSlot> getExamSlots() {
        return examSlots;
    }

    public List<ExamSession> getExamSessions() {
        return examSessions;
    }

    public Map<String, Course> getCoursesMap() {
        Map<String, Course> map = new HashMap<>();
        for (Course c : courses) {
            map.put(c.getCode(), c);
        }
        return map;
    }

    public Map<String, Student> getStudentsMap() {
        Map<String, Student> map = new HashMap<>();
        for (Student s : students) {
            map.put(s.getId(), s);
        }
        return map;
    }
}
