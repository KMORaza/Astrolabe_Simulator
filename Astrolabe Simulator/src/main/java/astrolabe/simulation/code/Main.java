package astrolabe.simulation.code;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        AstrolabeModel model = new AstrolabeModel();
        AstrolabeView view = new AstrolabeView(model);

        Scene scene = new Scene(view.getView(), 900, 800);

        // Apply global stylesheet
        scene.getStylesheets().add(getClass().getResource("/astrolabe/simulation/code/styles.css").toExternalForm());
        primaryStage.setTitle("Astrolabe Simulator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}