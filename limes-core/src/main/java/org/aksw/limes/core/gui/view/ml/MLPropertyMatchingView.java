package org.aksw.limes.core.gui.view.ml;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.aksw.limes.core.gui.model.Config;
import org.aksw.limes.core.ml.algorithm.eagle.util.PropertyMapping;

public class MLPropertyMatchingView {

	private ScrollPane rootPane;
	private ListView<String> sourcePropList;
	private ListView<String> targetPropList;
	private boolean sourcePropertyUnmatched = false;
	private boolean targetPropertyUnmatched = false;
	private Button cancelButton;
	private Button learnButton;
	private Stage stage;

	/**
	 * contains all the unmatchedPropertyBoxes where Properties have already
	 * been matched plus one empty/unfinished one
	 */
	private VBox matchedPropertiesBox;
	/**
	 * the downmost HBox inside matchedPropertiesBox is either empty or contains
	 * an unfinished matching
	 */
	private HBox unmatchedPropertyBox;
	private Config config;
	private MachineLearningView machineLearningView;

	public MLPropertyMatchingView(Config config, MachineLearningView mlv) {
		this.config = config;
		this.machineLearningView = mlv;
		createRootPane();
		addListeners();
	}

	public void createRootPane() {
		sourcePropList = new ListView<String>();
		targetPropList = new ListView<String>();
		sourcePropList.getItems()
				.addAll(config.getSourceInfo().getProperties());
		targetPropList.getItems()
				.addAll(config.getTargetInfo().getProperties());
		Label sourceLabel = new Label("available Source Properties:");
		Label targetLabel = new Label("available Target Properties:");
		Label addedSourceLabel = new Label("matched Source Properties:");
		Label addedTargetLabel = new Label("matched Target Properties:");
		VBox sourceColumn = new VBox();
		VBox targetColumn = new VBox();
		sourceColumn.getChildren().addAll(sourceLabel, sourcePropList);
		targetColumn.getChildren().addAll(targetLabel, targetPropList);
		Label propertyTypeLabel = new Label("PropertyType");
		HBox addedLabelBox = new HBox();
		addedLabelBox.getChildren().addAll(addedSourceLabel, addedTargetLabel,
				propertyTypeLabel);
		matchedPropertiesBox = new VBox();
		cancelButton = new Button("cancel");
		learnButton = new Button("learn");
		makeUnmatchedPropertyBox();
		learnButton.setDisable(true);
		HBox buttons = new HBox();
		buttons.getChildren().addAll(cancelButton, learnButton);
		BorderPane root = new BorderPane();
		HBox hb = new HBox();
		hb.getChildren().addAll(sourceColumn, targetColumn);
		HBox.setHgrow(sourceColumn, Priority.ALWAYS);
		HBox.setHgrow(targetColumn, Priority.ALWAYS);
		VBox topBox = new VBox();
		topBox.getChildren().addAll(hb, addedLabelBox);
		root.setTop(hb);
		root.setCenter(matchedPropertiesBox);
		root.setBottom(buttons);
		rootPane = new ScrollPane(root);
		rootPane.setFitToHeight(true);
		rootPane.setFitToWidth(true);
		Scene scene = new Scene(rootPane, 300, 400);
		scene.getStylesheets().add("gui/main.css");
		stage = new Stage();
		stage.setTitle("LIMES - Property Matching");
		stage.setScene(scene);
		stage.show();
	}

	private void addListeners() {
		cancelButton.setOnAction(e -> {
			stage.close();
		});

		learnButton
				.setOnAction(e -> {
					if (!sourcePropertyUnmatched && !targetPropertyUnmatched) {
						PropertyMapping propMap = new PropertyMapping();
						for (int i = 0; i < matchedPropertiesBox.getChildren()
								.size() - 1; i++) { // -1 because there is
													// always another empty
													// unmatchedPropertyBox
													// inside
					HBox row = (HBox) matchedPropertiesBox.getChildren().get(i);
					String sourceProp = ((Label) ((VBox) row.getChildren().get(
							0)).getChildren().get(0)).getText();
					String targetProp = ((Label) ((VBox) row.getChildren().get(
							0)).getChildren().get(0)).getText();
					String type = ((Spinner<String>) row.getChildren().get(2))
							.getValue();
					switch (type) {
					case "String":
						propMap.addStringPropertyMatch(sourceProp, targetProp);
						break;
					case "Number":
						propMap.addNumberPropertyMatch(sourceProp, targetProp);
						break;
					case "Date":
						propMap.addDatePropertyMatch(sourceProp, targetProp);
						break;
					case "Pointset":
						propMap.addPointsetPropertyMatch(sourceProp, targetProp);
						break;
					}
				}
				this.machineLearningView.getMlController().getMlModel()
						.getLearningsetting().setPropMap(propMap);
				this.machineLearningView.getMlController().learn(
						machineLearningView);
			}
		});
		sourcePropList.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (sourcePropList.getSelectionModel().getSelectedItem() != null) {
					if (sourcePropertyUnmatched
							|| (!sourcePropertyUnmatched && !targetPropertyUnmatched)) {
						Label sourceProp = new Label(sourcePropList
								.getSelectionModel().getSelectedItem());
						sourceProp.setAlignment(Pos.BASELINE_LEFT);
						((VBox) unmatchedPropertyBox.getChildren().get(0))
								.getChildren().add(sourceProp);
						sourcePropList.getItems().remove(
								sourcePropList.getSelectionModel()
										.getSelectedItem());

						if (unmatchedPropertyBox.getChildren().size() < 3) { //because always at least two VBoxes are contained
							addSpinnerAndButton();
						}
						if (sourcePropertyUnmatched) {
							sourcePropertyUnmatched = false;
							makeUnmatchedPropertyBox();
						} else {
							learnButton.setDisable(true);
							targetPropertyUnmatched = true;
						}
					}
				}
			}
		});

		targetPropList.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (targetPropList.getSelectionModel().getSelectedItem() != null) {
					if (targetPropertyUnmatched
							|| (!sourcePropertyUnmatched && !targetPropertyUnmatched)) {
						Label targetProp = new Label(targetPropList
								.getSelectionModel().getSelectedItem());
						targetProp.setAlignment(Pos.BASELINE_LEFT);
						((VBox) unmatchedPropertyBox.getChildren().get(1))
								.getChildren().add(targetProp);
						targetPropList.getItems().remove(
								targetPropList.getSelectionModel()
										.getSelectedItem());
						if (unmatchedPropertyBox.getChildren().size() < 3) {
							addSpinnerAndButton();
						}
						if (targetPropertyUnmatched) {
							targetPropertyUnmatched = false;
							makeUnmatchedPropertyBox();
						} else {
							learnButton.setDisable(true);
							sourcePropertyUnmatched = true;
						}
					}
				}
			}
		});
	}

	private void addSpinnerAndButton() {
		List<String> propertyTypeList = new ArrayList<String>();
		propertyTypeList.add("String");
		propertyTypeList.add("Number");
		propertyTypeList.add("Date");
		propertyTypeList.add("Pointset");
		ObservableList<String> obsPropTypeList = FXCollections
				.observableList(propertyTypeList);
		SpinnerValueFactory<String> svf = new SpinnerValueFactory.ListSpinnerValueFactory<>(
				obsPropTypeList);
		Spinner<String> propertyTypeSpinner = new Spinner<String>();
		propertyTypeSpinner.setValueFactory(svf);
		Button deleteRowButton = new Button("x");
		unmatchedPropertyBox.getChildren().addAll(propertyTypeSpinner,
				deleteRowButton);
		unmatchedPropertyBox.setAlignment(Pos.BOTTOM_RIGHT);
		unmatchedPropertyBox.setPrefWidth(stage.getWidth());
		deleteRowButton
				.setOnAction(e_ -> {
					HBox column = (HBox) deleteRowButton.getParent();
					String matchedSource = ((Label) ((VBox) column
							.getChildren().get(0)).getChildren().get(0))
							.getText();
					String matchedTarget = ((Label) ((VBox) column
							.getChildren().get(1)).getChildren().get(0))
							.getText();
					if (matchedSource != null) {
						sourcePropList.getItems().add(matchedSource);
					}
					if (matchedTarget != null) {
						targetPropList.getItems().add(matchedTarget);
					}
					VBox row = (VBox) deleteRowButton.getParent().getParent();
					row.getChildren().remove(deleteRowButton.getParent());
				});
	}

	private void makeUnmatchedPropertyBox() {
		learnButton.setDisable(false);
		sourcePropertyUnmatched = false;
		unmatchedPropertyBox = new HBox();
		unmatchedPropertyBox.setSpacing(10);
		VBox unmatchedProp1 = new VBox();
		unmatchedProp1.setFillWidth(true);
		unmatchedProp1.setAlignment(Pos.BASELINE_RIGHT);
		VBox unmatchedProp2 = new VBox();
		unmatchedProp2.setFillWidth(true);
		unmatchedProp2.setAlignment(Pos.BASELINE_RIGHT);
		unmatchedPropertyBox.getChildren().addAll(unmatchedProp1,
				unmatchedProp2);
		matchedPropertiesBox.getChildren().add(unmatchedPropertyBox);
	}
}