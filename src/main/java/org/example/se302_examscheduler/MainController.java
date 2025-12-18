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

    //Schedule Export
    @FXML
    private void handleExportSchedule(ActionEvent event) {
        if (schedule.getExamSessions().isEmpty()) {
            showError("Nothing to export", "Generate a schedule first.");
            return;
        }

        // 1) Choose format
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>("CSV", "CSV", "PDF");
        formatDialog.setTitle("Export Schedule");
        formatDialog.setHeaderText("Choose export format");
        formatDialog.initOwner(getOwnerStage());
        String format = formatDialog.showAndWait().orElse(null);
        if (format == null) return;

        // 2) Choose view
        ChoiceDialog<String> viewDialog = new ChoiceDialog<>("Course-based",
                "Course-based", "By Classroom", "By Student", "By Day");
        viewDialog.setTitle("Export Schedule");
        viewDialog.setHeaderText("Choose export view");
        viewDialog.initOwner(getOwnerStage());
        String view = viewDialog.showAndWait().orElse(null);
        if (view == null) return;

        // 3) Save file
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

    // ------------------- PDF Export -------------------

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
                // New page if needed
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

// ------------------- View dialogs -------------------

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

}
