package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    // נשתמש בערכים אלה כדי לקבוע n ו-t בפרויקט
    @Override
    public void init() throws Exception {
        // אתחול ברירת מחדל למקרה ולא הועברו פרמטרים
        String[] params = getParameters().getRaw().toArray(new String[0]);
        int maxAttempts = 3;
        int blockSeconds = 30;

        if (params.length >= 2) {
            try {
                maxAttempts = Integer.parseInt(params[0]);
                blockSeconds = Integer.parseInt(params[1]);
            } catch (NumberFormatException ignored) {}
        }

        AuthConfig.init(maxAttempts, blockSeconds);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Login Example - Lab3 (Threads & Blocking)");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
