package com.redwood.view;

import com.redwood.collections_api.IUserList;
import com.redwood.provider.UserListProvider;
import com.redwood.factory.UserFactory;
import com.redwood.interfaces.UserType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class MainController {
	private final Map<String, IUserList<UserType>> listsMap = new HashMap<>();
	private final UserListProvider.ImplType implType;
	private final UserFactory userFactory = new UserFactory();

	private UserType activeType;
	private IUserList<UserType> activeList;

	private final HBox nodesBox = new HBox(10);
	private final ComboBox<String> typesComboBox = new ComboBox<>();
	private final TextField valField = new TextField();
	private final TextField indexField = new TextField();
	private final Label getResultLabel = new Label("get result");
	private final Label iterLabel = new Label("Iterator: not started");

	private ListIterator<UserType> iterator;
	private int currentIndex = -1;

	public MainController(UserListProvider.ImplType type) {
		this.implType = type;
	}

	public Parent createView() {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10));

		VBox controls = new VBox(10);
		controls.setPrefWidth(220);

		typesComboBox.getItems().addAll(userFactory.getTypeNameList());
		typesComboBox.getSelectionModel().select(0);
		typesComboBox.setOnAction(e -> switchDataType());

		indexField.setPromptText("Index");
		valField.setPromptText("Value");

		Button addBtn = new Button("Add (End)");
		addBtn.setMaxWidth(Double.MAX_VALUE);
		addBtn.setOnAction(e -> add());
		Button addByIndBtn = new Button("Add by Index");
		addByIndBtn.setMaxWidth(Double.MAX_VALUE);
		addByIndBtn.setOnAction(e -> addByInd());
		Button removeBtn = new Button("Remove by Index");
		removeBtn.setMaxWidth(Double.MAX_VALUE);
		removeBtn.setOnAction(e -> removeByInd());

		Button getBtn = new Button("Get by Index");
		getBtn.setMaxWidth(Double.MAX_VALUE);
		getBtn.setOnAction(e -> getByIndex());

		Button sortBtn = new Button("Sort");
		sortBtn.setMaxWidth(Double.MAX_VALUE);
		sortBtn.setOnAction(e -> sort());
		Button clearBtn = new Button("Clear");
		clearBtn.setMaxWidth(Double.MAX_VALUE);
		clearBtn.setOnAction(e -> {
			activeList.clear();
			refreshList();
		});
		Button saveBtn = new Button("Save");
		saveBtn.setMaxWidth(Double.MAX_VALUE);
		saveBtn.setOnAction(e -> save());
		Button loadBtn = new Button("Load");
		loadBtn.setMaxWidth(Double.MAX_VALUE);
		loadBtn.setOnAction(e -> load());

		controls.getChildren().addAll(
			new Label("Data Type:"), typesComboBox, new Separator(),
			new Label("Operations:"), valField, indexField,
			addBtn, addByIndBtn, removeBtn, getBtn, sortBtn, clearBtn,
			new Separator(), saveBtn, loadBtn, getResultLabel
		);

		ScrollPane scrollPane = new ScrollPane(nodesBox);
		scrollPane.setFitToHeight(true);
		scrollPane.setStyle("-fx-background-color: transparent;");
		nodesBox.setAlignment(Pos.CENTER_LEFT);
		nodesBox.setPadding(new Insets(20));

		VBox bottomPanel = new VBox(10);
		bottomPanel.setAlignment(Pos.CENTER);
		bottomPanel.setPadding(new Insets(10));

		HBox navPanel = new HBox(10);
		navPanel.setAlignment(Pos.CENTER);
		Button btnFirst = new Button("|<");
		Button btnPrev = new Button("<");
		Button btnNext = new Button(">");
		Button btnLast = new Button(">|");

		btnFirst.setOnAction(e -> {
			iterator = activeList.listIterator();
			currentIndex = -1;
			updateNodes();
			updateIterLabel();
		});

		btnPrev.setOnAction(e -> {
			if (iterator.hasPrevious()) {
				iterator.previous();
				currentIndex = iterator.nextIndex();
				updateNodes();
				updateIterLabel();
			}
		});

		btnNext.setOnAction(e -> {
			if (iterator.hasNext()) {
				iterator.next();
				currentIndex = iterator.previousIndex();
				updateNodes();
				updateIterLabel();
			}
		});

		btnLast.setOnAction(e -> {
			while (iterator.hasNext()) {
				iterator.next();
				currentIndex = iterator.previousIndex();
			}
			updateNodes();
			updateIterLabel();
		});
		navPanel.getChildren().addAll(btnFirst, btnPrev, iterLabel, btnNext, btnLast);

		HBox iterOpsPanel = new HBox(10);
		iterOpsPanel.setAlignment(Pos.CENTER);
		Button iterSetBtn = new Button("Set via Iter");
		Button iterRemoveBtn = new Button("Remove via Iter");
		Button iterAddBtn = new Button("Add via Iter");

		iterSetBtn.setOnAction(e -> iterSet());
		iterRemoveBtn.setOnAction(e -> iterRemove());
		iterAddBtn.setOnAction(e -> iterAdd());

		iterOpsPanel.getChildren().addAll(iterSetBtn, iterRemoveBtn, iterAddBtn);
		bottomPanel.getChildren().addAll(navPanel, iterOpsPanel);

		root.setLeft(controls);
		root.setCenter(scrollPane);
		root.setBottom(bottomPanel);

		switchDataType();
		return root;
	}

	private void switchDataType() {
		activeType = userFactory.getBuilderByName(typesComboBox.getValue());
		activeList = listsMap.computeIfAbsent(activeType.typeName(), k -> UserListProvider.createList(implType));
		refreshList();
	}

	private void refreshList() {
		iterator = activeList.listIterator();
		currentIndex = -1;
		getResultLabel.setText("get result");
		updateNodes();
		updateIterLabel();
	}

	private void updateIterLabel() {
		iterLabel.setText("Iterator Index: " + currentIndex);
	}

	private void resetIterator() {
		iterator = activeList.listIterator();
		currentIndex = -1;
		updateNodes();
		updateIterLabel();
	}

	private void updateNodes() {
		nodesBox.getChildren().clear();
		for (int i = 0; i < activeList.size(); i++) {
			if (i > 0) {
				Label arrow = new Label(" <-> ");
				arrow.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
				nodesBox.getChildren().add(arrow);
			}

			UserType item = activeList.get(i);
			VBox nodeBox = new VBox(5);
			nodeBox.setAlignment(Pos.CENTER);

			Label valLabel = new Label(item.toString());
			valLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
			Label prevLabel = new Label("P: " + (i == 0 ? "null" : i - 1));
			Label nextLabel = new Label("N: " + (i == activeList.size() - 1 ? "null" : i + 1));

			if (i == currentIndex) {
				nodeBox.setStyle("-fx-border-color: #ff3333; -fx-border-width: 3; -fx-padding: 10; -fx-background-color: #ffe6e6;");
			} else {
				nodeBox.setStyle("-fx-border-color: #999999; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: #ffffff;");
			}

			nodeBox.getChildren().addAll(valLabel, prevLabel, nextLabel);
			nodesBox.getChildren().add(nodeBox);
		}
	}

	private void add() {
		try {
			String text = valField.getText();
			if (text.isEmpty()) return;
			UserType obj = (UserType) activeType.parseValue(text);
			activeList.add(obj);
			refreshList();
			valField.clear();
		} catch (Exception e) {
			showError("Add error: " + e.getMessage());
		}
	}

	private void addByInd() {
		try {
			int index = Integer.parseInt(indexField.getText().trim());
			UserType obj = (UserType) activeType.parseValue(valField.getText());
			activeList.add(index, obj);
			refreshList();
			valField.clear();
			indexField.clear();
		} catch (Exception e) {
			showError("Insert error: " + e.getMessage());
		}
	}

	private void removeByInd() {
		try {
			int index = Integer.parseInt(indexField.getText().trim());
			activeList.remove(index);
			refreshList();
			indexField.clear();
		} catch (Exception e) {
			showError("Remove error: " + e.getMessage());
		}
	}

	private void getByIndex() {
		try {
			int index = Integer.parseInt(indexField.getText().trim());
			UserType obj = activeList.get(index);
			getResultLabel.setText("Result: " + obj);
		} catch (Exception e) {
			showError("Get error: " + e.getMessage());
		}
	}

	private void sort() {
		try {
			//noinspection unchecked
			activeList.sort(activeType.typeComparator());
			refreshList();
		} catch (Exception e) {
			showError("Sort error: " + e.getMessage());
		}
	}

	private void iterSet() {
		try {
			if (currentIndex == -1) throw new IllegalStateException("Сначала переместите итератор!");
			UserType obj = (UserType) activeType.parseValue(valField.getText());
			iterator.set(obj);
			updateNodes();
			valField.clear();
		} catch (Exception e) {
			showError("Iterator Set error: " + e.getMessage());
		}
	}

	private void iterRemove() {
		try {
			if (currentIndex == -1) throw new IllegalStateException("Сначала переместите итератор!");
			iterator.remove();
			currentIndex = iterator.previousIndex();
			updateNodes();
			updateIterLabel();
		} catch (Exception e) {
			showError("Iterator Remove error: " + e.getMessage());
		}
	}

	private void iterAdd() {
		try {
			UserType obj = (UserType) activeType.parseValue(valField.getText());
			iterator.add(obj);
			// Новый элемент вставляется прямо перед курсором. previousIndex() укажет на него.
			currentIndex = iterator.previousIndex();
			updateNodes();
			updateIterLabel();
			valField.clear();
		} catch (Exception e) {
			showError("Iterator Add error: " + e.getMessage());
		}
	}

	private void save() {
		try {
			File dir = new File("userSaves");
			if (!dir.exists() && !dir.mkdirs()) {
				return;
			}

			File file = new File(dir, "userList_" + activeType.typeName() + ".xml");

			try (FileWriter wr = new FileWriter(file)) {
				activeList.save(wr);
			}
		} catch (Exception e) {
			showError("Save error: " + e.getMessage());
		}
	}

	private void load() {
		try {
			File file = new File("userSaves", "userList_" + activeType.typeName() + ".xml");

			try (FileReader rd = new FileReader(file)) {
				@SuppressWarnings("unchecked")
				Class<UserType> clazz = (Class<UserType>) activeType.getClass();
				activeList.load(rd, clazz);
				refreshList();
			}
		} catch (Exception e) {
			showError("Load error: " + e.getMessage());
		}
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
