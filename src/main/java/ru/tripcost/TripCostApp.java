package ru.tripcost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Точка входа JavaFX-приложения «Расчёт стоимости поездки».
 * Загружает главное окно (View), которое само создаёт свою ViewModel.
 */
public class TripCostApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("view/main-view.fxml")));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1140, 820);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("view/styles.css")).toExternalForm());

        stage.setTitle("Расчёт стоимости поездки");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
