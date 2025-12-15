package org.example.se302_examscheduler;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExamSchedulerEngine {

    public static SchedulingResult generateSchedule(Schedule schedule,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {

        schedule.getExamSlots().clear();
        schedule.getExamSessions().clear();

        List<ExamSlot> slots = generateSlots(startDate, endDate);
        schedule.getExamSlots().addAll(slots);


        List<Course> courses = new ArrayList<>(schedule.getCourses());
        courses.sort(Comparator.comparingInt((Course c) -> c.getStudents().size()).reversed());

        List<Course> unscheduled = new ArrayList<>();

        List<Classroom> classrooms = schedule.getClassrooms();
        if (classrooms.isEmpty()) {
            unscheduled.addAll(courses);
            return new SchedulingResult(schedule.getExamSessions(), unscheduled);
        }


        int nextRoomIndex = 0;

        for (Course course : courses) {
            boolean placed = false;
            int neededCapacity = course.getStudents().size();

            for (ExamSlot slot : slots) {
                if (placed) break;

                // Round-robin
                for (int offset = 0; offset < classrooms.size(); offset++) {
                    int currentIndex = (nextRoomIndex + offset) % classrooms.size();
                    Classroom classroom = classrooms.get(currentIndex);

                    if (classroom.getCapacity() >= neededCapacity
                            && isClassroomFreeAtSlot(schedule, classroom, slot)
                            && !hasConflict(schedule, course, slot)
                            && !violatesStudentConstraints(schedule, course, slot)) {

                        ExamSession session = new ExamSession(course, classroom, slot);
                        schedule.getExamSessions().add(session);
                        course.setExamSession(session);

                        // Bir sonraki yerleştirme için sınıf indexini kaydır
                        nextRoomIndex = (currentIndex + 1) % classrooms.size();
                        placed = true;
                        break;
                    }
                }
            }

            if (!placed) {
                unscheduled.add(course);
            }
        }

        return new SchedulingResult(schedule.getExamSessions(), unscheduled);
    }

   // no 2 consecutive slot
    private static boolean hasConflict(Schedule schedule, Course course, ExamSlot slot) {
        for (ExamSession session : schedule.getExamSessions()) {
            if (session.getSlot().equals(slot)) {
                for (Student s : course.getStudents()) {
                    if (session.getCourse().getStudents().contains(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

   // no same exam in a one class
    private static boolean isClassroomFreeAtSlot(Schedule schedule, Classroom classroom, ExamSlot slot) {
        for (ExamSession session : schedule.getExamSessions()) {
            if (session.getClassroom().equals(classroom) &&
                    session.getSlot().equals(slot)) {
                return false;
            }
        }
        return true;
    }

   // a student will have at most 2 exam in a day
    private static boolean violatesStudentConstraints(Schedule schedule, Course course, ExamSlot candidate) {
        for (Student s : course.getStudents()) {
            int examsThatDay = 0;

            for (ExamSession session : schedule.getExamSessions()) {
                if (!session.getCourse().getStudents().contains(s)) {
                    continue;
                }

                ExamSlot other = session.getSlot();

                if (other.getDate().equals(candidate.getDate())) {
                    examsThatDay++;


                    if (areConsecutive(other, candidate)) {
                        return true;
                    }
                }
            }

            // if there is more than 2 exam in a day it will be 3 and its forbidden
            if (examsThatDay >= 2) {
                return true;
            }
        }

        return false;
    }


    private static boolean areConsecutive(ExamSlot a, ExamSlot b) {
        if (!a.getDate().equals(b.getDate())) {
            return false;
        }
        return a.getEndTime().equals(b.getStartTime())
                || b.getEndTime().equals(a.getStartTime());
    }


    private static List<ExamSlot> generateSlots(LocalDate startDate, LocalDate endDate) {
        List<ExamSlot> slots = new ArrayList<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return slots;
        }

        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            slots.add(new ExamSlot(date, LocalTime.of(9, 0),  LocalTime.of(11, 0)));
            slots.add(new ExamSlot(date, LocalTime.of(11, 30), LocalTime.of(13, 30)));
            slots.add(new ExamSlot(date, LocalTime.of(14, 0),  LocalTime.of(16, 0)));
 
            date = date.plusDays(1);
        }
        return slots;
    }
}

