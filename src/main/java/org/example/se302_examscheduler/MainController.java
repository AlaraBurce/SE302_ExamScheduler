package org.example.se302_examscheduler;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    @FXML
    private void handleExportSchedule(ActionEvent event) {
        if (schedule.getExamSessions().isEmpty()) {
            showError("Nothing to export", "Generate a schedule first.");
            return;
        }

        ChoiceDialog<String> formatDialog = new ChoiceDialog<>("CSV", "CSV", "PDF");
        formatDialog.setTitle("Export Schedule");
        formatDialog.setHeaderText("Choose export format");
        formatDialog.initOwner(getOwnerStage());
        String format = formatDialog.showAndWait().orElse(null);
        if (format == null) return;

        ChoiceDialog<String> viewDialog = new ChoiceDialog<>("Course-based",
                "Course-based", "By Classroom", "By Student", "By Day");
        viewDialog.setTitle("Export Schedule");
        viewDialog.setHeaderText("Choose export view");
        viewDialog.initOwner(getOwnerStage());
        String view = viewDialog.showAndWait().orElse(null);
        if (view == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Export File");

        if ("PDF".equals(format)) {
            chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            chooser.setInitialFileName("schedule_export.pdf");
        } else {
            chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            chooser.setInitialFileName("schedule_export.csv");
        }

        File out = chooser.showSaveDialog(getOwnerStage());
        if (out == null) return;

        try {
            if ("PDF".equals(format)) {
                exportToPdf(out, view);
            } else {
                exportToCsv(out, view);
            }
            setStatus("Exported to " + out.getName());
            showInfo("Export", "Export completed: " + out.getName());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Export failed", e.toString());
        }
    }

    private void exportToCsv(File out, String view) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            switch (view) {
                case "By Classroom" -> exportByClassroom(pw);
                case "By Student" -> exportByStudent(pw);
                case "By Day" -> exportByDay(pw);
                default -> exportCourseBased(pw);
            }
        }
    }

    private void exportCourseBased(PrintWriter pw) {
        pw.println("Course,Classroom,Date,Start,End");
        schedule.getExamSessions().stream()
                .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                        .thenComparing(s -> s.getSlot().getStartTime())
                        .thenComparing(s -> s.getCourse().getCode()))
                .forEach(s -> pw.printf("%s,%s,%s,%s,%s%n",
                        s.getCourse().getCode(),
                        s.getClassroom().getName(),
                        s.getSlot().getDate(),
                        s.getSlot().getStartTime(),
                        s.getSlot().getEndTime()));
    }

    private void exportByClassroom(PrintWriter pw) {
        pw.println("Classroom,Course,Date,Start,End");
        schedule.getExamSessions().stream()
                .sorted(Comparator.comparing((ExamSession s) -> s.getClassroom().getName())
                        .thenComparing(s -> s.getSlot().getDate())
                        .thenComparing(s -> s.getSlot().getStartTime()))
                .forEach(s -> pw.printf("%s,%s,%s,%s,%s%n",
                        s.getClassroom().getName(),
                        s.getCourse().getCode(),
                        s.getSlot().getDate(),
                        s.getSlot().getStartTime(),
                        s.getSlot().getEndTime()));
    }

    private void exportByDay(PrintWriter pw) {
        pw.println("Date,Start,End,Course,Classroom");
        schedule.getExamSessions().stream()
                .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                        .thenComparing(s -> s.getSlot().getStartTime()))
                .forEach(s -> pw.printf("%s,%s,%s,%s,%s%n",
                        s.getSlot().getDate(),
                        s.getSlot().getStartTime(),
                        s.getSlot().getEndTime(),
                        s.getCourse().getCode(),
                        s.getClassroom().getName()));
    }

    private void exportByStudent(PrintWriter pw) {
        pw.println("Student,Course,Date,Start,End,Classroom");
        Map<String, List<ExamSession>> map = new HashMap<>();
        for (Student st : schedule.getStudents()) {
            List<ExamSession> sessions = new ArrayList<>();
            for (ExamSession s : schedule.getExamSessions()) {
                if (s.getCourse().getStudents().contains(st)) sessions.add(s);
            }
            sessions.sort(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                    .thenComparing(s -> s.getSlot().getStartTime()));
            map.put(st.getId(), sessions);
        }

        map.keySet().stream().sorted().forEach(sid -> {
            for (ExamSession s : map.get(sid)) {
                pw.printf("%s,%s,%s,%s,%s,%s%n",
                        sid,
                        s.getCourse().getCode(),
                        s.getSlot().getDate(),
                        s.getSlot().getStartTime(),
                        s.getSlot().getEndTime(),
                        s.getClassroom().getName());
            }
        });
    }


    private void exportToPdf(File out, String view) throws Exception {
        List<String> lines = buildExportLines(view);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 48f;
            float yStart = pageSize.getHeight() - margin;
            float leading = 14f;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Exam Schedule Export (" + view + ")");
            cs.newLineAtOffset(0, -leading * 1.6f);

            cs.setFont(PDType1Font.COURIER, 10);

            float y = yStart - (leading * 2.6f);

            for (String line : lines) {
                if (y <= margin) {
                    cs.endText();
                    cs.close();

                    page = new PDPage(pageSize);
                    doc.addPage(page);

                    cs = new PDPageContentStream(doc, page);
                    cs.beginText();
                    cs.setFont(PDType1Font.COURIER, 10);
                    cs.newLineAtOffset(margin, pageSize.getHeight() - margin);
                    y = pageSize.getHeight() - margin;
                }

                for (String wrapped : wrapLine(line, 95)) {
                    cs.showText(wrapped);
                    cs.newLineAtOffset(0, -leading);
                    y -= leading;

                    if (y <= margin) break;
                }
            }

            cs.endText();
            cs.close();

            doc.save(out);
        }
    }

    private List<String> buildExportLines(String view) {
        List<ExamSession> sessions = new ArrayList<>(schedule.getExamSessions());
        sessions.sort(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                .thenComparing(s -> s.getSlot().getStartTime())
                .thenComparing(s -> s.getCourse().getCode()));

        List<String> lines = new ArrayList<>();
        lines.add("--------------------------------------------------------------------------");

        switch (view) {
            case "By Classroom" -> {
                Map<String, List<ExamSession>> map = sessions.stream()
                        .collect(Collectors.groupingBy(s -> s.getClassroom().getName()));
                map.keySet().stream().sorted().forEach(room -> {
                    lines.add("== " + room + " ==");
                    map.get(room).stream()
                            .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                                    .thenComparing(s -> s.getSlot().getStartTime()))
                            .forEach(s -> lines.add(String.format("  %s | %s %s-%s",
                                    s.getCourse().getCode(),
                                    s.getSlot().getDate(),
                                    s.getSlot().getStartTime(),
                                    s.getSlot().getEndTime())));
                    lines.add("");
                });
            }
            case "By Student" -> {
                for (Student st : schedule.getStudents()) {
                    List<ExamSession> list = sessions.stream()
                            .filter(es -> es.getCourse().getStudents().contains(st))
                            .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                                    .thenComparing(s -> s.getSlot().getStartTime()))
                            .toList();

                    lines.add("== Student: " + st.getId() + " ==");
                    if (list.isEmpty()) {
                        lines.add("  (No exams)");
                    } else {
                        for (ExamSession s : list) {
                            lines.add(String.format("  %s | %s %s-%s | %s",
                                    s.getCourse().getCode(),
                                    s.getSlot().getDate(),
                                    s.getSlot().getStartTime(),
                                    s.getSlot().getEndTime(),
                                    s.getClassroom().getName()));
                        }
                    }
                    lines.add("");
                }
            }
            case "By Day" -> {
                Map<LocalDate, List<ExamSession>> map = sessions.stream()
                        .collect(Collectors.groupingBy(s -> s.getSlot().getDate()));
                map.keySet().stream().sorted().forEach(day -> {
                    lines.add("== " + day + " ==");
                    map.get(day).stream()
                            .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getStartTime()))
                            .forEach(s -> lines.add(String.format("  %s-%s | %s | %s",
                                    s.getSlot().getStartTime(),
                                    s.getSlot().getEndTime(),
                                    s.getCourse().getCode(),
                                    s.getClassroom().getName())));
                    lines.add("");
                });
            }
            default -> {
                lines.add("Course | Classroom | Date | Start-End");
                for (ExamSession s : sessions) {
                    lines.add(String.format("%s | %s | %s | %s-%s",
                            s.getCourse().getCode(),
                            s.getClassroom().getName(),
                            s.getSlot().getDate(),
                            s.getSlot().getStartTime(),
                            s.getSlot().getEndTime()));
                }
            }
        }

        lines.add("--------------------------------------------------------------------------");
        lines.add("Total sessions: " + sessions.size());
        return lines;
    }

    private List<String> wrapLine(String s, int maxLen) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        String text = s;

        while (text.length() > maxLen) {
            out.add(text.substring(0, maxLen));
            text = text.substring(maxLen);
        }
        out.add(text);
        return out;
    }


    @FXML
    private void handleViewByClassroom(ActionEvent event) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<ExamSession>> map = schedule.getExamSessions().stream()
                .collect(Collectors.groupingBy(s -> s.getClassroom().getName()));
        map.keySet().stream().sorted().forEach(room -> {
            sb.append("== ").append(room).append(" ==\n");
            map.get(room).stream()
                    .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                            .thenComparing(s -> s.getSlot().getStartTime()))
                    .forEach(s -> sb.append(String.format("  %s | %s %s-%s\n",
                            s.getCourse().getCode(),
                            s.getSlot().getDate(),
                            s.getSlot().getStartTime(),
                            s.getSlot().getEndTime())));
            sb.append("\n");
        });
        showLargeText("Schedule by Classroom", sb.toString());
    }

    @FXML
    private void handleViewByStudent(ActionEvent event) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Schedule by Student");
        dlg.setHeaderText("Enter Student ID");
        dlg.initOwner(getOwnerStage());
        String sid = dlg.showAndWait().orElse(null);
        if (sid == null || sid.trim().isEmpty()) return;

        Student st = schedule.getStudentsMap().get(sid.trim());
        if (st == null) {
            showError("Not found", "Student ID not found: " + sid);
            return;
        }

        List<ExamSession> sessions = schedule.getExamSessions().stream()
                .filter(s -> s.getCourse().getStudents().contains(st))
                .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                        .thenComparing(s -> s.getSlot().getStartTime()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(st.getId()).append("\n\n");
        for (ExamSession s : sessions) {
            sb.append(String.format("%s | %s %s-%s | %s\n",
                    s.getCourse().getCode(),
                    s.getSlot().getDate(),
                    s.getSlot().getStartTime(),
                    s.getSlot().getEndTime(),
                    s.getClassroom().getName()));
        }
        if (sessions.isEmpty()) sb.append("(No exams scheduled)\n");
        showLargeText("Student Schedule", sb.toString());
    }

    @FXML
    private void handleViewByDay(ActionEvent event) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<ExamSession>> map = schedule.getExamSessions().stream()
                .collect(Collectors.groupingBy(s -> s.getSlot().getDate().toString()));
        map.keySet().stream().sorted().forEach(day -> {
            sb.append("== ").append(day).append(" ==\n");
            map.get(day).stream()
                    .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getStartTime()))
                    .forEach(s -> sb.append(String.format("  %s-%s | %s | %s\n",
                            s.getSlot().getStartTime(),
                            s.getSlot().getEndTime(),
                            s.getCourse().getCode(),
                            s.getClassroom().getName())));
            sb.append("\n");
        });
        showLargeText("Schedule by Day", sb.toString());
    }
    @FXML
    private void handleManageClassrooms(ActionEvent event) {
        Stage owner = getOwnerStage();
        Stage stage = new Stage();
        stage.setTitle("Manage Classrooms");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        ObservableList<Classroom> rooms = FXCollections.observableArrayList(DatabaseManager.loadClassrooms());
        TableView<Classroom> table = new TableView<>(rooms);

        TableColumn<Classroom, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        idCol.setPrefWidth(180);

        TableColumn<Classroom, Number> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCapacity()));
        capCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, capCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button add = new Button("Add");
        Button edit = new Button("Edit");
        Button del = new Button("Delete");

        add.setOnAction(e -> {
            ClassroomForm form = ClassroomForm.show(owner, null);
            if (form == null) return;
            DatabaseManager.upsertClassroom(form.id, form.capacity);
            rooms.setAll(DatabaseManager.loadClassrooms());
            reloadFromDb();
        });

        edit.setOnAction(e -> {
            Classroom selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ClassroomForm form = ClassroomForm.show(owner, selected);
            if (form == null) return;
            DatabaseManager.upsertClassroom(form.id, form.capacity);
            rooms.setAll(DatabaseManager.loadClassrooms());
            reloadFromDb();
        });

        del.setOnAction(e -> {
            Classroom selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (!confirm("Delete classroom", "Delete " + selected.getName() + "?")) return;
            DatabaseManager.deleteClassroom(selected.getName());
            rooms.setAll(DatabaseManager.loadClassrooms());
            reloadFromDb();
        });

        HBox buttons = new HBox(10, add, edit, del);
        VBox root = new VBox(10, table, buttons);
        root.setStyle("-fx-padding: 12;");
        stage.setScene(new javafx.scene.Scene(root, 420, 420));
        stage.showAndWait();
    }

    @FXML
    private void handleManageStudents(ActionEvent event) {
        Stage owner = getOwnerStage();
        Stage stage = new Stage();
        stage.setTitle("Manage Students");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        ObservableList<Student> students = FXCollections.observableArrayList(DatabaseManager.loadStudents());
        TableView<Student> table = new TableView<>(students);

        TableColumn<Student, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        table.getColumns().add(idCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button add = new Button("Add");
        Button del = new Button("Delete");

        add.setOnAction(e -> {
            String id = prompt("Add Student", "Student ID:");
            if (id == null || id.trim().isEmpty()) return;
            DatabaseManager.upsertStudent(id.trim());
            students.setAll(DatabaseManager.loadStudents());
            reloadFromDb();
        });

        del.setOnAction(e -> {
            Student selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (!confirm("Delete student", "Delete " + selected.getId() + "?")) return;
            DatabaseManager.deleteStudent(selected.getId());
            students.setAll(DatabaseManager.loadStudents());
            reloadFromDb();
        });

        HBox buttons = new HBox(10, add, del);
        VBox root = new VBox(10, table, buttons);
        root.setStyle("-fx-padding: 12;");
        stage.setScene(new javafx.scene.Scene(root, 420, 420));
        stage.showAndWait();
    }

    @FXML
    private void handleManageCourses(ActionEvent event) {
        Stage owner = getOwnerStage();
        Stage stage = new Stage();
        stage.setTitle("Manage Courses");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        ObservableList<Course> courses = FXCollections.observableArrayList(DatabaseManager.loadCoursesShallow());
        TableView<Course> table = new TableView<>(courses);

        TableColumn<Course, String> codeCol = new TableColumn<>("Course Code");
        codeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        table.getColumns().add(codeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button add = new Button("Add");
        Button del = new Button("Delete");

        add.setOnAction(e -> {
            String code = prompt("Add Course", "Course code:");
            if (code == null || code.trim().isEmpty()) return;
            DatabaseManager.upsertCourse(code.trim());
            courses.setAll(DatabaseManager.loadCoursesShallow());
            reloadFromDb();
        });

        del.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (!confirm("Delete course", "Delete " + selected.getCode() + "?")) return;
            DatabaseManager.deleteCourse(selected.getCode());
            courses.setAll(DatabaseManager.loadCoursesShallow());
            reloadFromDb();
        });

        HBox buttons = new HBox(10, add, del);
        VBox root = new VBox(10, table, buttons);
        root.setStyle("-fx-padding: 12;");
        stage.setScene(new javafx.scene.Scene(root, 420, 420));
        stage.showAndWait();
    }

    @FXML
    private void handleManageEnrollments(ActionEvent event) {
        Stage owner = getOwnerStage();
        Stage stage = new Stage();
        stage.setTitle("Manage Enrollments");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        ObservableList<Course> courses = FXCollections.observableArrayList(DatabaseManager.loadCoursesShallow());
        ComboBox<Course> courseBox = new ComboBox<>(courses);
        courseBox.setPromptText("Select course");
        courseBox.setMaxWidth(Double.MAX_VALUE);

        ObservableList<String> enrolled = FXCollections.observableArrayList();
        TableView<String> table = new TableView<>(enrolled);
        TableColumn<String, String> sidCol = new TableColumn<>("Enrolled Student ID");
        sidCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()));
        table.getColumns().add(sidCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        courseBox.setOnAction(e -> {
            Course c = courseBox.getValue();
            if (c == null) return;
            enrolled.setAll(DatabaseManager.loadStudentIdsForCourse(c.getCode()));
        });

        Button add = new Button("Add Student");
        Button remove = new Button("Remove");

        add.setOnAction(e -> {
            Course c = courseBox.getValue();
            if (c == null) return;

            List<Student> all = DatabaseManager.loadStudents();
            List<String> already = DatabaseManager.loadStudentIdsForCourse(c.getCode());
            List<String> candidates = all.stream()
                    .map(Student::getId)
                    .filter(id -> !already.contains(id))
                    .sorted().toList();

            ChoiceDialog<String> pick = new ChoiceDialog<>(candidates.isEmpty() ? null : candidates.get(0), candidates);
            pick.setTitle("Add Enrollment");
            pick.setHeaderText("Select student to enroll in " + c.getCode());
            pick.initOwner(owner);
            String sid = pick.showAndWait().orElse(null);
            if (sid == null) return;

            DatabaseManager.addEnrollment(sid, c.getCode());
            enrolled.setAll(DatabaseManager.loadStudentIdsForCourse(c.getCode()));
            reloadFromDb();
        });

        remove.setOnAction(e -> {
            Course c = courseBox.getValue();
            String sid = table.getSelectionModel().getSelectedItem();
            if (c == null || sid == null) return;
            DatabaseManager.removeEnrollment(sid, c.getCode());
            enrolled.setAll(DatabaseManager.loadStudentIdsForCourse(c.getCode()));
            reloadFromDb();
        });

        HBox top = new HBox(10, new Label("Course:"), courseBox);
        HBox.setHgrow(courseBox, Priority.ALWAYS);

        HBox buttons = new HBox(10, add, remove);

        VBox root = new VBox(10, top, table, buttons);
        root.setStyle("-fx-padding: 12;");
        stage.setScene(new javafx.scene.Scene(root, 520, 480));
        stage.showAndWait();
    }

    private static class ClassroomForm {
        final String id;
        final int capacity;

        private ClassroomForm(String id, int capacity) {
            this.id = id;
            this.capacity = capacity;
        }

        static ClassroomForm show(Stage owner, Classroom existing) {
            Dialog<ClassroomForm> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "Add Classroom" : "Edit Classroom");
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);

            ButtonType ok = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

            TextField idField = new TextField(existing == null ? "" : existing.getName());
            TextField capField = new TextField(existing == null ? "40" : String.valueOf(existing.getCapacity()));

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.addRow(0, new Label("ID"), idField);
            grid.addRow(1, new Label("Capacity"), capField);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(bt -> {
                if (bt != ok) return null;
                String id = idField.getText() == null ? "" : idField.getText().trim();
                String cap = capField.getText() == null ? "" : capField.getText().trim();
                if (id.isEmpty()) return null;
                int c;
                try { c = Integer.parseInt(cap); } catch (Exception e) { return null; }
                return new ClassroomForm(id, Math.max(c, 0));
            });

            return dialog.showAndWait().orElse(null);
        }
    }

    private void reloadFromDb() {
        try {
            DatabaseManager.loadIntoSchedule(schedule);
            refreshScheduleTable();
            setStatus("Updated. " + summaryText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Reload failed", e.toString());
        }
    }
    private static class ValidationResult {
        final int issues;
        final String report;
        ValidationResult(int issues, String report) {
            this.issues = issues;
            this.report = report;
        }
    }

    private ValidationResult buildValidationReport() {
        StringBuilder report = new StringBuilder();
        int issues = 0;

        Map<String, Set<String>> roomSlot = new HashMap<>();
        for (ExamSession s : schedule.getExamSessions()) {
            String key = s.getClassroom().getName();
            String slot = s.getSlot().getDate() + " " + s.getSlot().getStartTime() + "-" + s.getSlot().getEndTime();
            roomSlot.putIfAbsent(key, new HashSet<>());
            if (!roomSlot.get(key).add(slot)) {
                issues++;
                report.append("Room double-booked: ").append(key).append(" @ ").append(slot).append("\n");
            }
            if (s.getClassroom().getCapacity() < s.getCourse().getStudents().size()) {
                issues++;
                report.append("Capacity issue: ").append(s.getCourse().getCode())
                        .append(" in ").append(key)
                        .append(" (").append(s.getCourse().getStudents().size())
                        .append(" students > ").append(s.getClassroom().getCapacity()).append(")\n");
            }
        }

        for (Student st : schedule.getStudents()) {
            List<ExamSession> sessions = schedule.getExamSessions().stream()
                    .filter(s -> s.getCourse().getStudents().contains(st))
                    .sorted(Comparator.comparing((ExamSession s) -> s.getSlot().getDate())
                            .thenComparing(s -> s.getSlot().getStartTime()))
                    .toList();

            Map<String, Integer> perDay = new HashMap<>();
            for (int i = 0; i < sessions.size(); i++) {
                ExamSlot slot = sessions.get(i).getSlot();
                String day = slot.getDate().toString();
                perDay.put(day, perDay.getOrDefault(day, 0) + 1);

                if (i > 0) {
                    ExamSlot prev = sessions.get(i - 1).getSlot();
                    if (prev.getDate().equals(slot.getDate())) {
                        int a = slotIndex(prev);
                        int b = slotIndex(slot);
                        if (a >= 0 && b >= 0 && Math.abs(a - b) == 1) {
                            issues++;
                            report.append("Back-to-back exams: ").append(st.getId())
                                    .append(" (").append(sessions.get(i - 1).getCourse().getCode())
                                    .append(" then ").append(sessions.get(i).getCourse().getCode()).append(") on ")
                                    .append(day).append("\n");
                        }
                    }
                }
            }

            for (var e : perDay.entrySet()) {
                if (e.getValue() > 2) {
                    issues++;
                    report.append("More than 2 exams/day: ").append(st.getId())
                            .append(" has ").append(e.getValue()).append(" exams on ").append(e.getKey()).append("\n");
                }
            }
        }

        if (issues == 0) return new ValidationResult(0, "No issues found. ✅");
        return new ValidationResult(issues, report.toString());
    }

    @FXML
    private void handleValidateSchedule(ActionEvent event) {
        if (schedule.getExamSessions().isEmpty()) {
            showInfo("Validate Schedule", "No sessions to validate. Generate a schedule first.");
            return;
        }

        ValidationResult vr = buildValidationReport();
        if (vr.issues == 0) {
            showInfo("Validate Schedule", "No issues found. ✅");
        } else {
            showLargeText("Validation Report (" + vr.issues + " issue(s))", vr.report);
        }
    }

}
