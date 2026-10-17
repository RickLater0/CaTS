module com.redwood.com.redwood.view {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires com.redwood.common;
    requires com.redwood.module;

    opens com.redwood.view to javafx.fxml;
}
