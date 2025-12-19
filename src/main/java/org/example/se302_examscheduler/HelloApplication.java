package org.example.se302_examscheduler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainView.fxml"));
        Scene scene = new Scene(root, 1100, 720);


        try {
            scene.getStylesheets().add(getClass().getResource("theme.css").toExternalForm());
        } catch (Exception ignored) {}

        primaryStage.setTitle("Exam Scheduler");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
