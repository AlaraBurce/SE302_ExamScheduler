package org.example.se302_examscheduler;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class MainController {
    private final Schedule schedule = new Schedule();

    @FXML
    private TableView<ExamSession> scheduleTable;
    @FXML private TableColumn<ExamSession, String> courseColumn;
    @FXML private TableColumn<ExamSession, String> classroomColumn;
    @FXML private TableColumn<ExamSession, String> dateColumn;
    @FXML private TableColumn<ExamSession, String> startTimeColumn;
    @FXML private TableColumn<ExamSession, String> endTimeColumn;
    @FXML private Label statusLabel;
    @FXML private TextField filterField;

    private final ObservableList<ExamSession> masterSessions = FXCollections.observableArrayList();

    public void initialize() {
        try {
            DatabaseManager.init();
            DatabaseManager.loadIntoSchedule(schedule);
        } catch (Exception e) {
            e.printStackTrace();
        }

        courseColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCourse().getCode()));
        classroomColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClassroom().getName()));
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getDate().toString()));
        startTimeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getStartTime().toString()));
        endTimeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlot().getEndTime().toString()));

        if (filterField != null) {
            filterField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        }

        refreshScheduleTable();
        setStatus("Ready. " + summaryText());
    }

    private String summaryText() {
        return "Courses: " + schedule.getCourses().size()
                + " | Students: " + schedule.getStudents().size()
                + " | Classrooms: " + schedule.getClassrooms().size()
                + " | Sessions: " + schedule.getExamSessions().size();
    }

    private Stage getOwnerStage() {
        if (scheduleTable != null && scheduleTable.getScene() != null) {
            return (Stage) scheduleTable.getScene().getWindow();
        }
        return null;
    }

    private int slotIndex(ExamSlot slot) {
        if (slot.getStartTime().equals(java.time.LocalTime.of(9, 0))) return 0;
        if (slot.getStartTime().equals(java.time.LocalTime.of(11, 30))) return 1;
        if (slot.getStartTime().equals(java.time.LocalTime.of(14, 0))) return 2;
        if (slot.getStartTime().equals(java.time.LocalTime.of(16, 30))) return 3;
        return -1;
    }

    private void setStatus(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private void refreshScheduleTable() {
        if (scheduleTable == null) return;
        masterSessions.setAll(schedule.getExamSessions());
        applyFilter();
    }

    private void applyFilter() {
        if (scheduleTable == null) return;
        String q = (filterField == null || filterField.getText() == null)
                ? ""
                : filterField.getText().trim().toLowerCase();

        if (q.isEmpty()) {
            scheduleTable.setItems(masterSessions);
            return;
        }

        ObservableList<ExamSession> filtered = FXCollections.observableArrayList();
        for (ExamSession s : masterSessions) {
            String hay = (s.getCourse().getCode() + " " +
                    s.getClassroom().getName() + " " +
                    s.getSlot().getDate() + " " +
                    s.getSlot().getStartTime() + "-" + s.getSlot().getEndTime()).toLowerCase();

            if (hay.contains(q)) filtered.add(s);
        }
        scheduleTable.setItems(filtered);
    }
}
