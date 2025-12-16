package org.example.demo1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

import java.time.LocalDate;

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

        progressBar.setProgress(0.1);

        SchedulingResult result = ExamSchedulerEngine.generateSchedule(schedule, start, end);

        progressBar.setProgress(1.0);

        StringBuilder sb = new StringBuilder();
        if (result.getUnscheduledCourses().isEmpty()) {
            sb.append("All courses have been scheduled successfully.");
        } else {
            sb.append("The following courses could not be scheduled.\n");
            sb.append("Please extend the date range or adjust data:\n\n");
            for (Course c : result.getUnscheduledCourses()) {
                sb.append("- ").append(c.getCode())
                        .append(" (students: ").append(c.getStudents().size()).append(")\n");
            }
        }

        messageArea.setText(sb.toString());
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Generate Schedule");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
