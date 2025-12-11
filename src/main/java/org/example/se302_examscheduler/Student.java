package org.example.se302_examscheduler;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private final String id;
    private final List<Course> courses = new ArrayList<>();

    public Student(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course c) {
        if (!courses.contains(c)) {
            courses.add(c);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
