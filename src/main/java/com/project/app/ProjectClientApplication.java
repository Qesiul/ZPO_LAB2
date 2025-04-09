package com.project.app;

import com.project.datasource.DbInitializer;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.controller.ProjectController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProjectClientApplication extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Ładujemy plik FXML
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/ProjectFrame.fxml"));

		// 1. Tworzymy DAO i przekazujemy do kontrolera
		ProjektDAO projektDAO = new ProjektDAOImpl();

		// 2. Ustawiamy fabrykę kontrolerów tak, aby wstrzykiwała DAO do konstruktora
		loader.setControllerFactory(controllerClass -> new ProjectController(projektDAO));

		Parent root = loader.load();

		// 3. Inicjalizacja Stage
		primaryStage.setTitle("Projekty");
		Scene scene = new Scene(root);
		// Podpinamy CSS
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		primaryStage.setScene(scene);

		// 4. Gdy zamykamy okno, wywołujemy shutdown() kontrolera (zamyka ExecutorService)
		ProjectController controller = loader.getController();
		primaryStage.setOnCloseRequest(event -> {
			controller.shutdown();
			Platform.exit();
		});

		primaryStage.show();
	}

	public static void main(String[] args) {
		// Najpierw inicjalizujemy strukturę bazy
		DbInitializer.init();
		// Następnie uruchamiamy JavaFX
		launch(args);
	}
}
