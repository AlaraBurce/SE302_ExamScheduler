package org.example.se302_examscheduler;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
public class ExportScheduleController {
    @FXML
    private RadioButton csvRadioButton;

    @FXML
    private RadioButton pdfRadioButton;

    @FXML
    private CheckBox byStudentCheckBox;

    @FXML
    private CheckBox byCourseCheckBox;

    @FXML
    private CheckBox byDayCheckBox;

    @FXML
    private TextField filePathField;

    @FXML
    private Label statusLabel;

    @FXML
    private ToggleGroup formatGroup;

    private Schedule schedule;

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }



    @FXML
    private void initialize() {

        formatGroup = new ToggleGroup();
        csvRadioButton.setToggleGroup(formatGroup);
        pdfRadioButton.setToggleGroup(formatGroup);
        csvRadioButton.setSelected(true);
    }
    @FXML
    private void handleBrowse(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select export file");
        if (csvRadioButton.isSelected()) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        }

        File file = chooser.showSaveDialog(getStage());
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
        }
    }



    @FXML
    private void handleExport(ActionEvent event) {
        if (schedule == null) {
            showError("Schedule model is not set.");
            return;
        }

        if (schedule.getExamSessions().isEmpty()) {
            showError("There is no schedule to export. Please generate a schedule first.");
            return;
        }

        String path = filePathField.getText();
        if (path == null || path.isBlank()) {
            showError("Please choose a file path.");
            return;
        }

        File file = new File(path);

        if (csvRadioButton.isSelected()) {
            try {
                exportAsCsv(file);
                setStatus("Exported schedule to " + file.getName());
            } catch (IOException e) {
                showError("Error exporting CSV: " + e.getMessage());
            }
        } else {
            showError("PDF export is not implemented in this version. Please use CSV.");
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        getStage().close();
    }

    private void exportAsCsv(File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("Course;Classroom;Date;Start;End");

            for (ExamSession session : schedule.getExamSessions()) {
                out.println(
                        session.getCourse().getCode() + ";" +
                                session.getClassroom().getName() + ";" +
                                session.getSlot().getDate() + ";" +
                                session.getSlot().getStartTime() + ";" +
                                session.getSlot().getEndTime()
                );
            }
        }
    }


    private Stage getStage() {
        return (Stage) filePathField.getScene().getWindow();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Export Schedule");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }
}
