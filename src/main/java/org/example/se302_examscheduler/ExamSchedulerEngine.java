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

        int nextRoomIndex = 0;

        for (Course course : courses) {
            boolean placed = false;
            int neededCapacity = course.getStudents().size();

            for (ExamSlot slot : slots) {
                if (placed) break;

                for (int offset = 0; offset < classrooms.size(); offset++) {
                    int idx = (nextRoomIndex + offset) % classrooms.size();
                    Classroom room = classrooms.get(idx);

                    if (room.getCapacity() >= neededCapacity
                            && isClassroomFreeAtSlot(schedule, room, slot)
                            && !hasConflict(schedule, course, slot)
                            && !violatesStudentConstraints(schedule, course, slot)) {

                        ExamSession session = new ExamSession(course, room, slot);
                        schedule.getExamSessions().add(session);
                        course.setExamSession(session);

                        nextRoomIndex = (idx + 1) % classrooms.size();
                        placed = true;
                        break;
                    }
                }
            }

            if (!placed) unscheduled.add(course);
        }

        return new SchedulingResult(schedule.getExamSessions(), unscheduled);
    }

    private static boolean hasConflict(Schedule schedule, Course course, ExamSlot slot) {
        for (ExamSession session : schedule.getExamSessions()) {
            if (!session.getSlot().equals(slot)) continue;

            for (Student s : course.getStudents()) {
                if (session.getCourse().getStudents().contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isClassroomFreeAtSlot(Schedule schedule, Classroom classroom, ExamSlot slot) {
        for (ExamSession session : schedule.getExamSessions()) {
            if (session.getClassroom().equals(classroom) && session.getSlot().equals(slot)) {
                return false;
            }
        }
        return true;
    }


    private static boolean violatesStudentConstraints(Schedule schedule, Course course, ExamSlot candidate) {
        for (Student s : course.getStudents()) {
            int examsThatDay = 0;

            for (ExamSession session : schedule.getExamSessions()) {
                if (!session.getCourse().getStudents().contains(s)) continue;

                ExamSlot other = session.getSlot();
                if (!other.getDate().equals(candidate.getDate())) continue;

                examsThatDay++;

                if (areConsecutive(other, candidate)) {
                    return true;
                }
            }

            if (examsThatDay >= 2) return true;
        }
        return false;
    }

    private static boolean areConsecutive(ExamSlot a, ExamSlot b) {
        if (!a.getDate().equals(b.getDate())) return false;

        int ia = slotIndex(a);
        int ib = slotIndex(b);


        if (ia < 0 || ib < 0) return false;


        return Math.abs(ia - ib) == 1;
    }

    private static int slotIndex(ExamSlot slot) {

        if (slot.getStartTime().equals(LocalTime.of(9, 0))) return 0;
        if (slot.getStartTime().equals(LocalTime.of(11, 30))) return 1;
        if (slot.getStartTime().equals(LocalTime.of(14, 0))) return 2;
        if (slot.getStartTime().equals(LocalTime.of(16, 30))) return 3;
        return -1;
    }


    private static List<ExamSlot> generateSlots(LocalDate startDate, LocalDate endDate) {
        List<ExamSlot> slots = new ArrayList<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) return slots;

        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            slots.add(new ExamSlot(date, LocalTime.of(9, 0), LocalTime.of(11, 0)));
            slots.add(new ExamSlot(date, LocalTime.of(11, 30), LocalTime.of(13, 30)));
            slots.add(new ExamSlot(date, LocalTime.of(14, 0), LocalTime.of(16, 0)));
            slots.add(new ExamSlot(date, LocalTime.of(16, 30), LocalTime.of(18, 30)));
            date = date.plusDays(1);
        }
        return slots;
    }
}
