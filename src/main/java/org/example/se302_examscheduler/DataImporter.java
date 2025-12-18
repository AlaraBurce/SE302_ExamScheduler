package org.example.se302_examscheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class DataImporter {

    public static void importClassrooms(File file, Schedule schedule) throws IOException {
        schedule.getClassrooms().clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("[,;]");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String capStr = parts[1].trim();
                    int capacity = Integer.parseInt(capStr);
                    schedule.getClassrooms().add(new Classroom(name, capacity));
                }
            }
        }

        DatabaseManager.replaceAllClassrooms(schedule.getClassrooms());
    }

    public static void importCourses(File file, Schedule schedule) throws IOException {
        schedule.getCourses().clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("[,;]");
                if (parts.length >= 1) {
                    String code = parts[0].trim();
                    if (!code.isEmpty()) {
                        schedule.getCourses().add(new Course(code));
                    }
                }
            }
        }

        DatabaseManager.replaceAllCourses(schedule.getCourses());
    }

    public static void importStudents(File file, Schedule schedule) throws IOException {
        schedule.getStudents().clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("[,;]");
                if (parts.length >= 1) {
                    String id = parts[0].trim();
                    if (!id.isEmpty()) {
                        schedule.getStudents().add(new Student(id));
                    }
                }
            }
        }

        DatabaseManager.replaceAllStudents(schedule.getStudents());
    }

    // Format: CourseCode_01;[S001,S002,...] or CourseCode_01,[S001,S002,...]
    public static void importAttendance(File file, Schedule schedule) throws IOException {
        Map<String, Course> coursesMap = schedule.getCoursesMap();
        Map<String, Student> studentsMap = schedule.getStudentsMap();

        for (Course c : schedule.getCourses()) {
            c.getStudents().clear();
        }
        for (Student s : schedule.getStudents()) {
            s.getCourses().clear();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("[,;]");
                if (parts.length < 2) continue;

                String courseCode = parts[0].trim();
                String studentsPart = parts[1].trim();

                Course course = coursesMap.get(courseCode);
                if (course == null) continue;

                studentsPart = studentsPart.replace("[", "").replace("]", "");
                String[] studentIds = studentsPart.split("\\s*\\|\\s*|\\s*,\\s*");

                for (String sid : studentIds) {
                    String studentId = sid.trim();
                    if (studentId.isEmpty()) continue;

                    Student student = studentsMap.get(studentId);
                    if (student != null) {
                        course.addStudent(student);
                        student.addCourse(course);
                    }
                }
            }
        }

        DatabaseManager.replaceAllEnrollments(schedule.getCourses());
    }
}