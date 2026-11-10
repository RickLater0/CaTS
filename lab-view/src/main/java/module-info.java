module com.redwood.view {
	// JavaFX
	requires javafx.base;
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;

	// Наши модули
	requires com.redwood.data;
	requires lab.impl.api;
	requires lab.impl.java;
	requires lab.impl.kotlin;
	requires lab.impl.scala;

	// Для работы FXML (рефлексия)
	opens com.redwood.view to javafx.fxml;
	opens com.redwood.launch to javafx.graphics, javafx.fxml;
	opens com.redwood.provider to javafx.fxml; // на всякий случай
}
