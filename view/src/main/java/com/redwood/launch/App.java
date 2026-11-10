package com.redwood.launch;

import com.redwood.collections.UserListProvider;
import com.redwood.view.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class App extends Application {
	@Override
	public void start(Stage primaryStage) {
		List<String> args = getParameters().getRaw();
		String impl = args.isEmpty() ? null : args.getFirst();

		if (impl == null) {
			ChoiceDialog<String> dialog = new ChoiceDialog<>("Java", "Java", "Scala", "Kotlin");
			dialog.setTitle("Select Implementation");
			dialog.setHeaderText("Choose the underlying list implementation:");
			Optional<String> result = dialog.showAndWait();
			result.ifPresentOrElse(
				choice -> showMainWindow(primaryStage, choice),
				() -> System.exit(0)
			);
		} else {
			showMainWindow(primaryStage, impl);
		}
	}

	private void showMainWindow(Stage stage, String implName) {
		try {
			UserListProvider.ImplType type = UserListProvider.ImplType.valueOf(implName.toUpperCase());
			MainController controller = new MainController(type);
			Scene scene = new Scene(controller.createView(), 1000, 600);
			stage.setScene(scene);
			stage.setTitle("LR - " + implName + " Implementation");
			stage.show();
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Launch Error");
			alert.setContentText("Failed to launch: " + e.getMessage());
			alert.showAndWait();
		}
	}
}
