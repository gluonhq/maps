package com.gluonhq;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/contact.fxml"));
        Parent parent = fxmlLoader.load();
        primaryStage.setScene(new Scene(parent, 800, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        Main.launch(args);
    }
}