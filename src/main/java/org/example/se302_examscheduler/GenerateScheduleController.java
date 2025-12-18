package org.example.se302_examscheduler;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class GenerateScheduleController {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextArea messageArea;

    private Schedule schedule;

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @FXML
    private void handleGenerate(ActionEvent event) {
        System.out.println(">>> GENERATE CLICKED");
        messageArea.appendText("\n>>> GENERATE CLICKED\n");

        if (schedule == null) {
            showError("Schedule model is not set.");
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showError("Please select both start and end dates.");
            return;
        }

        if (end.isBefore(start)) {
            showError("End date cannot be before start date.");
            return;
        }

        if (schedule.getCourses().isEmpty()
                || schedule.getClassrooms().isEmpty()
                || schedule.getStudents().isEmpty()) {
            showError("Please import classrooms, courses, students and attendance first.");
            return;
        }

        progressBar.setProgress(0.15);

        SchedulingResult result = ExamSchedulerEngine.generateSchedule(schedule, start, end);

        progressBar.setProgress(1.0);

        messageArea.setText("DEBUG: Generate clicked. Sessions=" + schedule.getExamSessions().size()
                + " | Unscheduled=" + (result == null ? -1 : result.getUnscheduledCourses().size()));

        if (result != null && !result.getUnscheduledCourses().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("⚠ Some courses could not be scheduled.\n")
                    .append("Please extend the date range or adjust data.\n\n");
            for (Course c : result.getUnscheduledCourses()) {
                sb.append("- ").append(c.getCode())
                        .append(" (students: ").append(c.getStudents().size()).append(")\n");
            }
            messageArea.setText(sb.toString());

            showAlert(Alert.AlertType.WARNING,
                    "Schedule Warning",
                    "Some courses could not be scheduled",
                    "Some courses could not be scheduled.\n\nCheck Messages for details.");
            return;
        }

        ValidationReport report = validateScheduleByStudentId(schedule);

        if (report.issues > 0) {
            messageArea.setText("❌ Conflicts detected (" + report.issues + "):\n\n" + report.details);

            showAlert(Alert.AlertType.ERROR,
                    "Schedule Conflicts",
                    "Conflicts detected",
                    "Conflicts were found.\n\nCheck Messages for details.");
        } else {
            messageArea.setText("✅ Schedule generated successfully.\nNo conflicts detected.\n"
                    + "- No consecutive slots\n- Max 2 exams/day");

            showAlert(Alert.AlertType.INFORMATION,
                    "Schedule OK",
                    "No conflicts detected",
                    "All constraints satisfied.");
        }
    }
    private static class ValidationReport {
        int issues;
        String details;
        ValidationReport(int issues, String details) {
            this.issues = issues;
            this.details = details;
        }
    }
    private ValidationReport validateScheduleByStudentId(Schedule schedule) {
        int issues = 0;
        StringBuilder out = new StringBuilder();

        Map<String, List<ExamSession>> map = new HashMap<>();

        for (ExamSession es : schedule.getExamSessions()) {
            for (Student st : es.getCourse().getStudents()) {
                map.computeIfAbsent(st.getId(), k -> new ArrayList<>()).add(es);
            }
        }

        for (Map.Entry<String, List<ExamSession>> entry : map.entrySet()) {
            String studentId = entry.getKey();
            List<ExamSession> sessions = entry.getValue();

            sessions.sort(Comparator
                    .comparing((ExamSession s) -> s.getSlot().getDate())
                    .thenComparing(s -> slotIndex(s.getSlot()))
            );

            Map<LocalDate, Integer> perDay = new HashMap<>();
            for (ExamSession es : sessions) {
                perDay.merge(es.getSlot().getDate(), 1, Integer::sum);
            }
            for (Map.Entry<LocalDate, Integer> e : perDay.entrySet()) {
                if (e.getValue() > 2) {
                    issues++;
                    out.append("Max 2/day violated: ")
                            .append(studentId)
                            .append(" has ").append(e.getValue())
                            .append(" exams on ").append(e.getKey())
                            .append("\n");
                }
            }

            for (int i = 1; i < sessions.size(); i++) {
                ExamSession prevS = sessions.get(i - 1);
                ExamSession curS = sessions.get(i);

                ExamSlot prev = prevS.getSlot();
                ExamSlot cur = curS.getSlot();

                if (prev.getDate().equals(cur.getDate())) {
                    int a = slotIndex(prev);
                    int b = slotIndex(cur);
                    if (a >= 0 && b >= 0 && Math.abs(a - b) == 1) {
                        issues++;
                        out.append("Consecutive slots violated: ")
                                .append(studentId)
                                .append(" (")
                                .append(prevS.getCourse().getCode())
                                .append(" then ")
                                .append(curS.getCourse().getCode())
                                .append(") on ")
                                .append(cur.getDate())
                                .append("\n");
                    }
                }
            }
        }

        if (issues == 0) return new ValidationReport(0, "No issues.");
        return new ValidationReport(issues, out.toString());
    }

    private int slotIndex(ExamSlot slot) {
        if (slot.getStartTime().equals(LocalTime.of(9, 0))) return 0;
        if (slot.getStartTime().equals(LocalTime.of(11, 30))) return 1;
        if (slot.getStartTime().equals(LocalTime.of(14, 0))) return 2;
        if (slot.getStartTime().equals(LocalTime.of(16, 30))) return 3;
        return -1;
    }

private void showAlert(Alert.AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);

    Window owner = (messageArea != null && messageArea.getScene() != null)
            ? messageArea.getScene().getWindow()
            : null;

    if (owner != null) {
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
    }

    alert.showAndWait();
}

private void showError(String msg) {
    showAlert(Alert.AlertType.ERROR, "Error", "Generate Schedule", msg);
}
private void showPopup(String title, String text) {
    javafx.stage.Stage stage = new javafx.stage.Stage();
    stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
    stage.setAlwaysOnTop(true);
    stage.setTitle(title);

    javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
    lbl.setWrapText(true);

    javafx.scene.control.Button ok = new javafx.scene.control.Button("OK");
    ok.setOnAction(e -> stage.close());

    javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(12, lbl, ok);
    root.setStyle("-fx-padding: 16; -fx-background-color: white;");
    stage.setScene(new javafx.scene.Scene(root, 420, 180));
    stage.showAndWait();
}
}
