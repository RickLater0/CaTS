package com.redwood.view;

import java.util.*;

import com.redwood.UserFactory;
import com.redwood.interfaces.UserType;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class Controller {
    @FXML
    private Button addButton;

    @FXML
    private Button addByIndButton;

    @FXML
    private TextField addIndexField;

    @FXML
    private Button clearButton;

    @FXML
    private TextField getIndexField;

    @FXML
    private Label getResultLabel;

    @FXML
    private ListView<String> listView;

    @FXML
    private Button loadButton;

    @FXML
    private Button removeButton;

    @FXML
    private Button removeByIndButton;

    @FXML
    private TextField removeIndexField;

    @FXML
    private Button saveButton;

    @FXML
    private Button sortButton;

    @FXML
    private ComboBox<String> typesComboBox;

    @FXML
    private TextField valField;

    Map<String, UserType> typesMap;
    UserFactory userFactory;
    List<String> types ;

    @FXML
    void add(ActionEvent event) {

    }

    @FXML
    void addByInd(ActionEvent event) {

    }

    @FXML
    void clear(ActionEvent event) {

    }

    @FXML
    void load(ActionEvent event) {

    }

    @FXML
    void remove(ActionEvent event) {

    }

    @FXML
    void removeByInd(ActionEvent event) {

    }

    @FXML
    void save(ActionEvent event) {

    }

    @FXML
    void sort(ActionEvent event) {

    }

    @FXML
    void initialize() {
        userFactory = new UserFactory();
        typesMap = new HashMap<>();
        types = new ArrayList<>(userFactory.getTypeNameList());

        typesComboBox.getItems().addAll(types);
        typesComboBox.getSelectionModel().select(0);

        for(String t : types){
            typesMap.put(t, userFactory.getBuilderByName(t));
        }
    }
}
