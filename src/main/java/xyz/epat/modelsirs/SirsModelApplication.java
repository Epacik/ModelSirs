package xyz.epat.modelsirs;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SirsModelApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        //loading main window of the program from FXML

        // fxml defines main UI and have a controller (MainViewController.java) attached to it
        // the whole simulation happens there
        FXMLLoader fxmlLoader = new FXMLLoader(SirsModelApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 450);

        stage.setTitle("Model SIRS v1.0 - Damian Kuśmierz");

        // stop simulation if user is trying to close the window
        stage.setOnCloseRequest(we -> {
            ((MainViewController)fxmlLoader.getController()).stop();
            Platform.exit();
            try {
                this.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //throw new RuntimeException("Why are you still running?");
        });
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinHeight(550);
        stage.setMinWidth(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}