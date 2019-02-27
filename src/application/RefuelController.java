package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;

import application.RefuelModel;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class RefuelController {

	@FXML
	private TextField tfFileRoot;

	@FXML
	private Button btnGetData;

	@FXML
	private ComboBox<String> cbFuelType;

	@FXML
	private BarChart<String, Number> chartOutput;

	@FXML
	private CategoryAxis yAxis;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private Label lblError;

	private ArrayList<RefuelModel> refuelingData = new ArrayList<RefuelModel>();
	// months in an array
	public String[] months = new DateFormatSymbols().getShortMonths();

	@FXML
	void getData(ActionEvent event) {
		try {
			if (checkRoot()) {
				this.refuelingData = getRefuelingData();
				if (this.refuelingData != null) {
					// activate ComboBox with fuel types
					activateFuelSelection(this.refuelingData);
					clearErrors();
				}
			} else
				throw new IOException();
		} catch (IOException e) {
			setError("icorrectPath");
		}
	}

	@FXML
	void selectFuelType(ActionEvent event) {
		// clear BarChart
		chartOutput.getData().clear();
		chartOutput.layout();

		// get selected fuel type
		String selectedFuelType = cbFuelType.getSelectionModel().getSelectedItem().toString();

		// filter data by selected fuel type
		ArrayList<RefuelModel> filteredRefuelings = filterRefuelingsByType(this.refuelingData, selectedFuelType);

		// populate the BarChart
		popupateBarChartReport(filteredRefuelings, selectedFuelType);
	}

	// checks file exist in root or not
	private boolean checkRoot() {
		boolean isValid = false;

		if (!tfFileRoot.getText().isEmpty()) {
			File file = new File(tfFileRoot.getText());

			if (file.isFile() && file.getAbsoluteFile().exists()) {
				isValid = true;
			}
		}
		return isValid;
	}

	// get refueling data from file and put in ArrayList
	public ArrayList<RefuelModel> getRefuelingData() {
		ArrayList<RefuelModel> refuelingData = new ArrayList<RefuelModel>();

		File file = new File(tfFileRoot.getText());
		Path path = file.toPath();

		Scanner s = null;
		try {
			s = new Scanner(path);
			while (s.hasNext()) {
				// data in each line is divided by symbol "|"
				String[] line = s.next().split("\\|");

				// set the price and amount to the proper format
				String priceFormatted = line[1].replace(",", ".");
				String amountFormatted = line[2].replace(",", ".");

				// if negative value, show an error
				if (Double.parseDouble(priceFormatted) < 0)
					throw new IOException();

				// if negative value, show an error
				if (Double.parseDouble(amountFormatted) < 0)
					throw new IOException();

				// find a month from the date string
				String[] calend = line[3].split("\\.");
				int month = Integer.parseInt(calend[1]);

				// add to ArrayList
				refuelingData.add(new RefuelModel(line[0], Double.parseDouble(priceFormatted),
						Double.parseDouble(amountFormatted), month));

			}

			return refuelingData;

		} catch (IOException e) {

			setError("parseError");

			return null;
		} finally {
			s.close();
		}

	}

	private void activateFuelSelection(ArrayList<RefuelModel> refuelData)

	{
		cbFuelType.setItems(getFuelTypes(refuelData));
		cbFuelType.setDisable(false);
	}

	// get the fuel types without duplication
	private ObservableList<String> getFuelTypes(ArrayList<RefuelModel> refuelData)

	{
		ObservableList<String> data = FXCollections.observableArrayList();

		for (RefuelModel model : refuelData) {
			String fuelName = model.getName();

			if (!data.contains(fuelName))
				data.add(fuelName);
		}
		// the option to see total expense
		data.add("all");

		return data;
	}

	// clear errors
	private void clearErrors() {
		if (!lblError.getText().isEmpty())
			lblError.setText("");
	}

	// return refueling data by fuel type
	private ArrayList<RefuelModel> filterRefuelingsByType(ArrayList<RefuelModel> refuelings, String refuelType) {
		if (refuelType == "all")
			return refuelings;

		ArrayList<RefuelModel> filteredRefuelings = new ArrayList<RefuelModel>();

		for (RefuelModel model : refuelings) {
			if (model.getName().equals(refuelType))
				filteredRefuelings.add(model);
		}

		return filteredRefuelings;
	}

	// populate BarChart by selected fuel type
	private void popupateBarChartReport(ArrayList<RefuelModel> refuelings, String selectedFuelType) {
		// sort refueling according to month Jan to Dec
		ArrayList<RefuelModel> refuelingsSorted = sortRefuelings(refuelings, selectedFuelType);

		// find maximal and minimal values for coloring
		double maxValue = findMaxValue(refuelingsSorted);
		double minValue = findMinValue(refuelingsSorted, maxValue);

		ObservableList<XYChart.Series<String, Number>> barChartData = FXCollections.observableArrayList();
		final BarChart.Series<String, Number> refuelingSeries = new BarChart.Series<String, Number>();

		refuelingSeries.setName("Refueling Data by months");
		String monthName = "";
		double monthPrice = 0.000;
		for (RefuelModel model : refuelingsSorted) {
			monthName = getMonthNameByNumber(model.getMonth());
			monthPrice = model.getPrice() * model.getAmount();

			DecimalFormat numberFormat = new DecimalFormat("0.000");
			Text dataText = new Text(numberFormat.format(monthPrice) + " €");

			final XYChart.Data<String, Number> data = new Data<String, Number>(monthName, monthPrice);
			data.nodeProperty().addListener(new ChangeListener<Node>() {
				@Override
				public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
					setNodeStyle(data, maxValue, minValue);

					displayLabelForData(data, dataText);
				}
			});

			refuelingSeries.getData().add(data);
		}

		barChartData.add(refuelingSeries);
		chartOutput.setData(barChartData);
	}

	// sort refueling data from Jan to Dec
	private ArrayList<RefuelModel> sortRefuelings(ArrayList<RefuelModel> refuelings, String selectedFuelType) {
		ArrayList<RefuelModel> refuelingsSorted = new ArrayList<RefuelModel>();

		// populate an array with empty RefuelModel objects, one for each month
		for (int monthCount = 1; monthCount < 13; monthCount++) {
			RefuelModel model = new RefuelModel(selectedFuelType, 0.000, 0.000, monthCount);
			refuelingsSorted.add(model);
		}

		// for each refueling, calculate and update the month
		for (RefuelModel model : refuelings) {
			int index = model.getMonth() - 1;
			RefuelModel updatedRefuelModel = refuelingsSorted.get(index);
			updatedRefuelModel.addPrice(model.getPrice());
			updatedRefuelModel.addAmount(model.getAmount());
		}

		return refuelingsSorted;
	}

	// find minimal value
	private double findMinValue(ArrayList<RefuelModel> refuelingsSorted, double maxValue) {
		double minValue = maxValue;

		double sum = 0;
		for (RefuelModel model : refuelingsSorted) {
			sum = model.getAmount() * model.getPrice();
			if (sum < minValue && sum != 0)
				minValue = sum;
		}

		return minValue;
	}

	// find maximal value
	private double findMaxValue(ArrayList<RefuelModel> refuelingsSorted) {
		double maxValue = 0;

		double sum = 0;
		for (RefuelModel model : refuelingsSorted) {
			sum = model.getAmount() * model.getPrice();
			if (sum > maxValue)
				maxValue = sum;
		}

		return maxValue;
	}

	// get month name by number
	private String getMonthNameByNumber(int month) {
		return months[month - 1];
	}

	// set color for each bar according to their value
	private void setNodeStyle(XYChart.Data<String, Number> data, double maxValue, double minValue) {
		Node node = data.getNode();

		if (data.getYValue().doubleValue() == maxValue) {
			node.setStyle("-fx-bar-fill: red;");
		} else if (data.getYValue().doubleValue() == minValue) {
			node.setStyle("-fx-bar-fill: green;");
		} else {
			node.setStyle("-fx-bar-fill: yellow;");
		}
	}

	// set the price over each bar
	private void displayLabelForData(XYChart.Data<String, Number> data, Text dataText) {
		final Node node = data.getNode();

		node.parentProperty().addListener(new ChangeListener<Parent>() {
			@Override
			public void changed(ObservableValue<? extends Parent> ov, Parent oldParent, Parent parent) {
				if (parent != null) {
					Group parentGroup = (Group) parent;

					if (parentGroup != null)
						parentGroup.getChildren().add(dataText);
				}
			}
		});

		node.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
			@Override
			public void changed(ObservableValue<? extends Bounds> ov, Bounds oldBounds, Bounds bounds) {
				dataText.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - dataText.prefWidth(-1) / 2));
				dataText.setLayoutY(Math.round(bounds.getMinY() - dataText.prefHeight(-1) * 0.5));
			}
		});
	}

	// setting errors by alias
	private void setError(String errorAlias) {
		cbFuelType.setDisable(true);
		cbFuelType.getItems().clear();
		chartOutput.getData().clear();

		switch (errorAlias) {
		case "icorrectPath":
			lblError.setText("An input file or its path is specified empty or incorrect.");
			break;
		case "parseError":
			lblError.setText("Some input data are in a wrong format (negative value, wrong date format etc).");
			break;
		}
	}

}
