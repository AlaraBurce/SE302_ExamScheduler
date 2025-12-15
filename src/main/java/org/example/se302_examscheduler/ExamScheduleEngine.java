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
        // Eski slot ve oturumları temizle
        schedule.getExamSlots().clear();
        schedule.getExamSessions().clear();

        List<ExamSlot> slots = generateSlots(startDate, endDate);
        schedule.getExamSlots().addAll(slots);

        // Dersleri, öğrenci sayısına göre büyükten küçüğe sırala
        List<Course> courses = new ArrayList<>(schedule.getCourses());
        courses.sort(Comparator.comparingInt((Course c) -> c.getStudents().size()).reversed());

        List<Course> unscheduled = new ArrayList<>();

        List<Classroom> classrooms = schedule.getClassrooms();
        if (classrooms.isEmpty()) {
            unscheduled.addAll(courses);
            return new SchedulingResult(schedule.getExamSessions(), unscheduled);
        }

        // Sınıfları sırayla kullanmak için index
        int nextRoomIndex = 0;

        for (Course course : courses) {
            boolean placed = false;
            int neededCapacity = course.getStudents().size();

            for (ExamSlot slot : slots) {
                if (placed) break;

                // Round-robin: sınıfları hep farklı birinden başlatarak dene
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

    /**
     * Aynı slotta, öğrencisi çakışan dersler olamaz.
     */
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

    /**
     * Aynı sınıfta, aynı slotta birden fazla sınav olamaz.
     */
    private static boolean isClassroomFreeAtSlot(Schedule schedule, Classroom classroom, ExamSlot slot) {
        for (ExamSession session : schedule.getExamSessions()) {
            if (session.getClassroom().equals(classroom) &&
                    session.getSlot().equals(slot)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 1) Öğrencinin bir günde en fazla 2 sınavı olabilir.
     * 2) Aynı gün içinde back-to-back (art arda) sınavı olamaz.
     */
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

                    // Aynı gün ardışık slotlar mı?
                    if (areConsecutive(other, candidate)) {
                        return true;
                    }
                }
            }

            // Aynı günde zaten 2 sınavı varsa, bu ders 3. olur → yasak
            if (examsThatDay >= 2) {
                return true;
            }
        }

        return false;
    }

    /**
     * İki slot aynı gün ve bitiş/başlangıç saatleri dokunduğunda "consecutive" say.
     */
    private static boolean areConsecutive(ExamSlot a, ExamSlot b) {
        if (!a.getDate().equals(b.getDate())) {
            return false;
        }
        return a.getEndTime().equals(b.getStartTime())
                || b.getEndTime().equals(a.getStartTime());
    }

    /**
     * Sınav dönemi için slot üretimi.
     * İstersen buradaki saatleri kendi istediğin değerlere göre değiştirebilirsin.
     */
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
            // İstersen 4. slot:
            // slots.add(new ExamSlot(date, LocalTime.of(16, 30), LocalTime.of(18, 30)));
            date = date.plusDays(1);
        }
        return slots;
    }
}

