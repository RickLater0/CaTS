package com.redwood.view;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.redwood.factory.UserFactory;
import com.redwood.interfaces.UserType;

import com.redwood.list.UserList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

    @FXML
    private Button getButton;

    private static final String saveDirName = "userSaves";
    private static final String saveFileName = "userList";

    Map<String, UserType> typesMap;
    UserFactory userFactory;
    List<String> types;
    UserType activeType;
    Map<UserType, UserList<UserType>> listMap;
    UserList<UserType> activeList;

    @FXML
    void add(ActionEvent event) {
        try {
            String text = valField.getText();
            UserType obj = (UserType) activeType.parseValue(text);
            activeList.add(obj);
            updateListView();
            valField.clear();
        } catch (Exception e) {
            showError("Add err: " + e.getMessage());
        }
    }

    @FXML
    void addByInd(ActionEvent event) {
        try {
            int index = Integer.parseInt(addIndexField.getText().trim());
            String text = valField.getText();
            UserType obj = (UserType) activeType.parseValue(text);
            activeList.add(index, obj);
            updateListView();
            valField.clear();
            addIndexField.clear();
        } catch (Exception e) {
            showError("Insert err: " + e.getMessage());
        }
    }

    @FXML
    void clear(ActionEvent event) {
        activeList.clear();
        updateListView();
    }

    @FXML
    void remove(ActionEvent event) {
        try {
            UserType obj = (UserType) activeType.parseValue(removeIndexField.getText().trim());
            activeList.remove(obj);
            updateListView();
            getIndexField.clear();
        } catch (Exception e) {
            showError("Remove err: " + e.getMessage());
        }
    }

    @FXML
    void removeByInd(ActionEvent event) {
        try {
            int index = Integer.parseInt(removeIndexField.getText().trim());
            activeList.remove(index);
            updateListView();
            removeIndexField.clear();
        } catch (Exception e) {
            showError("Remove by ind err: " + e.getMessage());
        }
    }

    @FXML
    void get(ActionEvent event) {
        try {
            int index = Integer.parseInt(getIndexField.getText().trim());
            UserType obj = activeList.get(index);
            getResultLabel.setText("Res: " + obj.toString());
        } catch (Exception e) {
            getResultLabel.setText("Get err: " + e.getMessage());
        }
    }

    @FXML
    void save(ActionEvent event) {
        File saveDir = new File(saveDirName);
        if(saveDir.isDirectory() || saveDir.mkdir()){
            String fileName = saveDirName + "/" + saveFileName + "_" + activeType.typeName() + ".xml";
            File sf = new File(fileName);
            try (FileWriter wr = new FileWriter(sf)) {
                UserList.save(wr, activeList);
            } catch (IOException e) {
                showError("Save err: " + e.getMessage());
            }
        }
    }

    @FXML
    void load(ActionEvent event) {
        String fileName = saveDirName + "/" + saveFileName + "_" + activeType.typeName() + ".xml";
        File sf = new File(fileName);
        if (!sf.exists()) {
            showError("File not found: " + sf.getName());
            return;
        }

        try (FileReader rd = new FileReader(sf)) {
            @SuppressWarnings("unchecked")
            Class<UserType> clazz = (Class<UserType>) activeType.getClass();
            UserList<UserType> loadedList = UserList.load(rd, clazz);

            activeList.clear();
            activeList.addAll(loadedList);
            updateListView();
        } catch (Exception e) {
            showError("Load err: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @FXML
    void sort(ActionEvent event) {
        activeList.sort(activeType.typeComparator());
        updateListView();
    }

    @FXML
    void typeChoose(ActionEvent event){
        activeType = userFactory.getBuilderByName(typesComboBox.getValue());
        activeList = listMap.get(activeType);
        updateListView();
    }

    @FXML
    void initialize() {
        userFactory = new UserFactory();
        typesMap = new HashMap<>();
        types = new ArrayList<>(userFactory.getTypeNameList());
        listMap = new HashMap<>();

        typesComboBox.getItems().addAll(types);
        typesComboBox.getSelectionModel().select(0);

        for(String t : types){
            var usr = userFactory.getBuilderByName(t);
            typesMap.put(t, usr);
            listMap.put(usr, new UserList<>());
        }

        activeType = userFactory.getBuilderByName(typesComboBox.getValue());
        activeList = listMap.get(activeType);
    }

    private void updateListView() {
        listView.getItems().clear();
        for (UserType item : activeList) {
            listView.getItems().add(item.toString());
        }

        if (listView.getScene() != null && listView.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("LR1 — " + activeType.typeName() + " [Size: " + activeList.size() + "]");
        }
        clearFields();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Err");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields(){
        addIndexField.clear();
        getIndexField.clear();
        removeIndexField.clear();
        valField.clear();
    }
}
