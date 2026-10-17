package com.redwood.launch;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        URL resource = getClass().getResource("mainWindow.fxml");
        if (resource == null) {
            throw new Exception("Resource not found");
        }
        Scene scene = new Scene(FXMLLoader.load(resource));
        Stage stage = new Stage();
        stage.setScene(scene);
    }
}
