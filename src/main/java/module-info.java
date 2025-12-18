module org.example.se302_examscheduler {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.se302_examscheduler to javafx.fxml;
    exports org.example.se302_examscheduler;
}