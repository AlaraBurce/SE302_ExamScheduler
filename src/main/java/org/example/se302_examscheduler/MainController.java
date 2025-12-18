package org.example.se302_examscheduler;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
    @FXML
    private void handleImportClassrooms(ActionEvent event) {
        File f = chooseCsv("Import Classrooms CSV");
        if (f == null) return;

        try {
            DataImporter.importClassrooms(f, schedule);
            DatabaseManager.loadIntoSchedule(schedule);
            refreshScheduleTable();
            setStatus("Classrooms imported. " + summaryText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Import failed", e.toString());
        }
    }

    @FXML
    private void handleImportCourses(ActionEvent event) {
        File f = chooseCsv("Import Courses CSV");
        if (f == null) return;

        try {
            DataImporter.importCourses(f, schedule);
            DatabaseManager.loadIntoSchedule(schedule);
            refreshScheduleTable();
            setStatus("Courses imported. " + summaryText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Import failed", e.toString());
        }
    }

    @FXML
    private void handleImportStudents(ActionEvent event) {
        File f = chooseCsv("Import Students CSV");
        if (f == null) return;

        try {
            DataImporter.importStudents(f, schedule);
            DatabaseManager.loadIntoSchedule(schedule);
            refreshScheduleTable();
            setStatus("Students imported. " + summaryText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Import failed", e.toString());
        }
    }

    @FXML
    private void handleImportAttendance(ActionEvent event) {
        File f = chooseCsv("Import Attendance Lists CSV");
        if (f == null) return;

        try {
            DataImporter.importAttendance(f, schedule);
            DatabaseManager.loadIntoSchedule(schedule);
            refreshScheduleTable();
            setStatus("Attendance imported. " + summaryText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Import failed", e.toString());
        }
    }

    private File chooseCsv(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return chooser.showOpenDialog(getOwnerStage());
    }
    @FXML
    private void handleGenerateSchedule(ActionEvent event) {
        Dialog<LocalDate[]> dialog = new Dialog<>();
        dialog.setTitle("Generate Schedule");
        dialog.initOwner(getOwnerStage());
        dialog.initModality(Modality.WINDOW_MODAL);

        ButtonType generateBtn = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateBtn, ButtonType.CANCEL);

        DatePicker startPicker = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker endPicker = new DatePicker(LocalDate.now().plusDays(5));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Start date"), 0, 0);
        grid.add(startPicker, 1, 0);
        grid.add(new Label("End date"), 0, 1);
        grid.add(endPicker, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == generateBtn) return new LocalDate[]{startPicker.getValue(), endPicker.getValue()};
            return null;
        });

        LocalDate[] res = dialog.showAndWait().orElse(null);
        if (res == null) return;

        if (schedule.getCourses().isEmpty() || schedule.getStudents().isEmpty() || schedule.getClassrooms().isEmpty()) {
            showError("Missing data",
                    "Import Classrooms, Courses, Students and Attendance Lists before generating a schedule.");
            return;
        }

        try {
            SchedulingResult result = ExamSchedulerEngine.generateSchedule(schedule, res[0], res[1]);
            DatabaseManager.saveExamSessions(schedule.getExamSessions());
            refreshScheduleTable();

            if (result != null && !result.getUnscheduledCourses().isEmpty()) {
                String list = result.getUnscheduledCourses().stream()
                        .map(Course::getCode).sorted().collect(Collectors.joining(", "));
                showInfo("Generated with warnings",
                        "Some courses could not be scheduled with the strict constraints.\nUnscheduled: " + list);
                setStatus("Generated (with unscheduled courses). " + summaryText());
            } else {
                setStatus("Schedule generated. " + summaryText());
            }

            ValidationResult vr = buildValidationReport();
            if (vr.issues == 0) {
                showInfo("Schedule OK", "No issues found. ✅");
            } else {
                showLargeText("Validation Report (" + vr.issues + " issue(s))", vr.report);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Schedule generation failed", e.toString());
        }
    }
}
