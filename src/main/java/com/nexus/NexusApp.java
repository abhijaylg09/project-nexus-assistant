package com.nexus;

import com.nexus.core.NexusCore;
import com.nexus.ui.HudController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX Application for Project N.E.X.U.S.
 */
public class NexusApp extends Application {

    private NexusCore core;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Initialize Central Java Core
            core = new NexusCore();
            core.start();

            // 2. Initialize HUD Controller
            HudController hudController = new HudController(core);

            // 3. Create Scene
            Scene scene = new Scene(hudController.getRoot(), 1180, 780);

            // 4. Attach Cyberpunk Sci-Fi Stylesheet
            URL cssUrl = getClass().getResource("/styles/hud-cyberpunk.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("[NexusApp] Warning: /styles/hud-cyberpunk.css not found on classpath.");
            }

            // 5. Configure Primary Stage
            primaryStage.setTitle("PROJECT N.E.X.U.S • Personal Real-Time AI Assistant [Java Orchestrator]");
            primaryStage.setMinWidth(1080);
            primaryStage.setMinHeight(700);
            primaryStage.setScene(scene);

            // 6. Graceful Shutdown on Close
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("[NexusApp] Window closing, initiating graceful shutdown...");
                if (core != null) {
                    core.stop();
                }
                System.exit(0);
            });

            primaryStage.show();
            System.out.println("[NexusApp] JavaFX HUD Interface launched successfully.");

        } catch (Exception e) {
            System.err.println("[NexusApp] Fatal error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
