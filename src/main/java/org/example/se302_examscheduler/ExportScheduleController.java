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

    @FXML
    private void initialize() {

        formatGroup = new ToggleGroup();
        csvRadioButton.setToggleGroup(formatGroup);
        pdfRadioButton.setToggleGroup(formatGroup);
        csvRadioButton.setSelected(true);
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
