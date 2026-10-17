module com.redwood.view {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires com.redwood.module;
    requires com.redwood.common;

    opens com.redwood.launch to javafx.graphics, javafx.fxml;
    opens com.redwood.view to javafx.fxml;
}
