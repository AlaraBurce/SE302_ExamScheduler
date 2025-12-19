package org.example.se302_examscheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


public final class DatabaseManager {

    private static final String DB_FILE_NAME = "exam_scheduler.db";
    private static final String DB_DIR_NAME = ".exam-scheduler";
    private static final String DB_URL;

    static {
        String home = System.getProperty("user.home");
        Path dir = Paths.get(home, DB_DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        DB_URL = "jdbc:sqlite:" + dir.resolve(DB_FILE_NAME).toAbsolutePath();
    }

    private DatabaseManager() {}

    public static void init() {
        try (Connection c = DriverManager.getConnection(DB_URL);
             Statement st = c.createStatement()) {

            st.executeUpdate("""
                PRAGMA foreign_keys = ON;
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS classrooms (
                    id TEXT PRIMARY KEY,
                    capacity INTEGER NOT NULL CHECK(capacity >= 0)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS students (
                    id TEXT PRIMARY KEY
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS courses (
                    code TEXT PRIMARY KEY
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    student_id TEXT NOT NULL,
                    course_code TEXT NOT NULL,
                    PRIMARY KEY(student_id, course_code),
                    FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE,
                    FOREIGN KEY(course_code) REFERENCES courses(code) ON DELETE CASCADE
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS exam_sessions (
                    course_code TEXT PRIMARY KEY,
                    classroom_id TEXT NOT NULL,
                    date TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    FOREIGN KEY(course_code) REFERENCES courses(code) ON DELETE CASCADE,
                    FOREIGN KEY(classroom_id) REFERENCES classrooms(id) ON DELETE RESTRICT
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }



    public static void replaceAllClassrooms(List<Classroom> rooms) {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM classrooms");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO classrooms(id, capacity) VALUES(?, ?)")) {
                for (Classroom r : rooms) {
                    ps.setString(1, r.getName());
                    ps.setInt(2, r.getCapacity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
        clearExamSessions();
    }

    public static void replaceAllStudents(List<Student> students) {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM students");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO students(id) VALUES(?)")) {
                for (Student s : students) {
                    ps.setString(1, s.getId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
        clearExamSessions();
    }

    public static void replaceAllCourses(List<Course> courses) {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM courses");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO courses(code) VALUES(?)")) {
                for (Course course : courses) {
                    ps.setString(1, course.getCode());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
        clearExamSessions();
    }

    public static void replaceAllEnrollments(List<Course> courses) {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM enrollments");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO enrollments(student_id, course_code) VALUES(?, ?)")) {
                for (Course course : courses) {
                    for (Student s : course.getStudents()) {
                        ps.setString(1, s.getId());
                        ps.setString(2, course.getCode());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        });
        clearExamSessions();
    }

    public static void saveExamSessions(List<ExamSession> sessions) {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM exam_sessions");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO exam_sessions(course_code, classroom_id, date, start_time, end_time) VALUES(?, ?, ?, ?, ?)")) {
                for (ExamSession s : sessions) {
                    ps.setString(1, s.getCourse().getCode());
                    ps.setString(2, s.getClassroom().getName());
                    ps.setString(3, s.getSlot().getDate().toString());
                    ps.setString(4, s.getSlot().getStartTime().toString());
                    ps.setString(5, s.getSlot().getEndTime().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
    }

    public static void clearExamSessions() {
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM exam_sessions");
            }
        });
    }



    public static List<Classroom> loadClassrooms() {
        return queryList("SELECT id, capacity FROM classrooms ORDER BY id",
                rs -> new Classroom(rs.getString(1), rs.getInt(2)));
    }

    public static void upsertClassroom(String id, int capacity) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO classrooms(id, capacity) VALUES(?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET capacity=excluded.capacity")) {
                ps.setString(1, id);
                ps.setInt(2, capacity);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static void deleteClassroom(String id) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM classrooms WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static List<Student> loadStudents() {
        return queryList("SELECT id FROM students ORDER BY id",
                rs -> new Student(rs.getString(1)));
    }

    public static void upsertStudent(String id) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO students(id) VALUES(?) ON CONFLICT(id) DO NOTHING")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static void deleteStudent(String id) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM students WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static List<Course> loadCoursesShallow() {
        return queryList("SELECT code FROM courses ORDER BY code",
                rs -> new Course(rs.getString(1)));
    }

    public static void upsertCourse(String code) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO courses(code) VALUES(?) ON CONFLICT(code) DO NOTHING")) {
                ps.setString(1, code);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static void deleteCourse(String code) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM courses WHERE code=?")) {
                ps.setString(1, code);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static List<String> loadStudentIdsForCourse(String courseCode) {
        return queryList("SELECT student_id FROM enrollments WHERE course_code=? ORDER BY student_id",
                ps -> ps.setString(1, courseCode),
                rs -> rs.getString(1));
    }

    public static void addEnrollment(String studentId, String courseCode) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO enrollments(student_id, course_code) VALUES(?, ?)")) {
                ps.setString(1, studentId);
                ps.setString(2, courseCode);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }

    public static void removeEnrollment(String studentId, String courseCode) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM enrollments WHERE student_id=? AND course_code=?")) {
                ps.setString(1, studentId);
                ps.setString(2, courseCode);
                ps.executeUpdate();
            }
        });
        clearExamSessions();
    }



    public static void loadIntoSchedule(Schedule schedule) {
        schedule.getExamSessions().clear();
        schedule.getExamSlots().clear();
        schedule.getCourses().clear();
        schedule.getStudents().clear();
        schedule.getClassrooms().clear();


        List<Classroom> rooms = loadClassrooms();
        schedule.getClassrooms().addAll(rooms);

        List<Student> students = loadStudents();
        schedule.getStudents().addAll(students);


        List<Course> courses = loadCoursesShallow();
        schedule.getCourses().addAll(courses);


        var studentsMap = schedule.getStudentsMap();
        var coursesMap = schedule.getCoursesMap();


        queryVoid("SELECT student_id, course_code FROM enrollments",
                rs -> {
                    String sid = rs.getString(1);
                    String ccode = rs.getString(2);
                    Student s = studentsMap.get(sid);
                    Course c = coursesMap.get(ccode);
                    if (s != null && c != null) {
                        s.addCourse(c);
                        c.addStudent(s);
                    }
                });


        queryVoid("SELECT course_code, classroom_id, date, start_time, end_time FROM exam_sessions",
                rs -> {
                    String ccode = rs.getString(1);
                    String roomId = rs.getString(2);
                    LocalDate d = LocalDate.parse(rs.getString(3));
                    LocalTime st = LocalTime.parse(rs.getString(4));
                    LocalTime et = LocalTime.parse(rs.getString(5));
                    Course c = coursesMap.get(ccode);
                    Classroom room = schedule.getClassrooms().stream()
                            .filter(r -> r.getName().equals(roomId))
                            .findFirst().orElse(null);
                    if (c != null && room != null) {
                        ExamSlot slot = new ExamSlot(d, st, et);
                        ExamSession session = new ExamSession(c, room, slot);
                        schedule.getExamSessions().add(session);
                        c.setExamSession(session);
                    }
                });
    }



    private interface TxBody { void run(Connection c) throws SQLException; }

    private static void inTransaction(TxBody body) {
        init();
        try (Connection c = DriverManager.getConnection(DB_URL)) {
            c.setAutoCommit(false);
            try {
                body.run(c);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB transaction failed: " + e.getMessage(), e);
        }
    }

    private interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
    private interface PreparedBinder { void bind(PreparedStatement ps) throws SQLException; }
    private interface RowConsumer { void accept(ResultSet rs) throws SQLException; }

    private static <T> List<T> queryList(String sql, RowMapper<T> mapper) {
        init();
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<T> out = new ArrayList<>();
            while (rs.next()) out.add(mapper.map(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + e.getMessage(), e);
        }
    }

    private static <T> List<T> queryList(String sql, PreparedBinder binder, RowMapper<T> mapper) {
        init();
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + e.getMessage(), e);
        }
    }

    private static void queryVoid(String sql, RowConsumer consumer) {
        init();
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) consumer.accept(rs);
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + e.getMessage(), e);
        }
    }
}
