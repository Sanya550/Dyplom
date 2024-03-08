package com.example.dyplom;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    //todo: change parameters for difusion
    //berliand page 43, when H = 100 meters, stan = 3;
    private static final double alfaDifusion = 0.78;
    private static final double aDifusion = 0.32;
    private static final double betaDifusion = 0.78;
    private static final double bDifusion = 0.22;

    //кількість опадів за місяць
    private static final double quantityOfPrecipation = 88d;

    public static double[][] windData;
    public static Coordinate coordinate;
    public static double uValue;
    public static double qValue;

    @FXML
    private Label oneLabel;

    @FXML
    private Label twoLabel;

    @FXML
    private Label threeLabel;

    @FXML
    private Label fourLabel;

    @FXML
    private Label fiveLabel;

    @FXML
    private Label sixLabel;

    @FXML
    private Label sevenLabel;

    @FXML
    private Label eightLabel;

    @FXML
    private TextField distanceField;

    @FXML
    private TextField heightSourceField;

    @FXML
    private ComboBox<String> comboBoxForVector;

    @FXML
    private ComboBox<Integer> comboBoxForDays;

    @FXML
    private ComboBox<Integer> comboBoxForH;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var optionsForVector = FXCollections.observableArrayList(
                "1 - Північ",
                "2 - Північно-східний",
                "3 - Східний",
                "4 - Південно-східний",
                "5 - Південний",
                "6 - Південно-західний",
                "7 - Західний",
                "8 - Північно-західний"
        );
        comboBoxForVector.setItems(optionsForVector);
        comboBoxForVector.setValue(optionsForVector.get(0));

        var optionsForH = FXCollections.observableArrayList(50, 100, 200);
        comboBoxForH.setItems(optionsForH);
        comboBoxForH.setValue(optionsForH.get(1));
    }

    @FXML
    public void windData() {
        var fileopen = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileopen.showDialog(null, "Виберіть текстовий файл з даними вітру");
        File file = fileopen.getSelectedFile();
        String s = file.getPath();
        List<String> listString = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Path.of(s))) {
            listString = br.lines().collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        listString = listString.stream().map(v -> v.replaceAll("\\s+", " ").trim()).collect(Collectors.toList());
        windData = Helper.convertToDoubleArray(listString);

        //filling comboBoxForDays:
        if (windData.length > 0) {
            ObservableList<Integer> days = FXCollections.observableArrayList();
            for (int i = 1; i <= windData.length; i++) {
                days.add(i);
            }
            comboBoxForDays.setItems(days);
            comboBoxForDays.setValue(days.get(0));
        }
    }

    @FXML
    public void find() {
        var height = Double.parseDouble(heightSourceField.getText());
        readCoordinate();
        readWindValue();
        readPowerValue();
        var cValue = getCValueForCoordinate(coordinate, qValue, uValue, height);
        JOptionPane.showMessageDialog(null, "Result = " + cValue, "Result", JOptionPane.INFORMATION_MESSAGE);
    }

    //С - концентрация в некоторой точке с координатами x; y; z; q – мощность выброса, г/с;
    //Н – высота виртуального источника; u - средняя скорость ветра, м/с;
    private double getCValueForCoordinate(Coordinate coordinate, double q, double u, double h) {
        var difusionY = getDifusionForY(coordinate.getX());
        var difusionZ = getDifusionForZ(coordinate.getX());
        double firstPart = q / (2 * Math.PI * u);
        if (difusionY != 0){
            firstPart /= difusionY;
        }
        if (difusionZ != 0){
            firstPart /= difusionZ;
        }
        double partByY = Math.pow(Math.E, (-Math.pow(coordinate.getY(), 2) / (2 * Math.pow(getDifusionForY(coordinate.getX()), 2))));
        double partByZ = Math.pow(Math.E, (-Math.pow(coordinate.getZ() - h, 2) / (2 * Math.pow(getDifusionForY(coordinate.getX()), 2)))) +
                (Math.pow(Math.E, (-Math.pow(coordinate.getZ() + h, 2) / (2 * Math.pow(getDifusionForY(coordinate.getX()), 2)))));
        var fOp = getFunctionRozpodilByKvantil(getKvantilForKoefOfOpad(), coordinate.getX(), u);
        var fChemistry = getFunctionRozpodilByKvantil(getKvantilForKoefOfChemistry(), coordinate.getX(), u);
        double c = firstPart * partByY * partByZ * fOp * fChemistry;
        return c;
    }

    //горизонтальна дифузія
    private double getDifusionForY(double x) {
        return aDifusion * Math.pow(x, alfaDifusion);//TODO: HERE WILL BE DISTANCE
    }

    //вертикальна дифузія
    private double getDifusionForZ(double x) {
        return bDifusion * Math.pow(x, betaDifusion);
    }

    private double getKvantilForKoefOfChemistry() {
        return 14 * 0.000001 + 3 * 0.000001;//todo: clarify formula one more time. Shall I add SO2 and NO2?
    }

    private double getKvantilForKoefOfOpad() {
        var i = quantityOfPrecipation / (24 * 30);
        var kvantilOp = i <= 0.2 ? 11.8 * Math.pow(i, 0.9) * Math.pow(Math.E, -2 * i) : 7 * Math.pow(i - 0.1, 0.575);
        kvantilOp *= 0.00001;
        return kvantilOp;
    }

    private double getFunctionRozpodilByKvantil(double kvantil, double x, double u) {
        return Math.pow(Math.E, -kvantil * x / u);//todo: clarify or it depends on H(popov formula number 9)
    }

    private void readCoordinate() {
        double distance = Double.parseDouble(distanceField.getText());
        double z = comboBoxForH.getValue();
        double x, y;
        if (comboBoxForVector.getValue().startsWith("1") || comboBoxForVector.getValue().startsWith("5")) {
            y = distance;
            x = 0d;
        } else if (comboBoxForVector.getValue().startsWith("3") || comboBoxForVector.getValue().startsWith("7")) {
            y = 0d;
            x = distance;
        } else {
            x = distance / Math.sqrt(2);
            y = distance / Math.sqrt(2);
        }
        coordinate = new Coordinate(x, y, z);
    }

    private void readWindValue() {
        var windVal = Integer.parseInt(String.valueOf(comboBoxForVector.getValue().charAt(0))) - 1;
        var day = comboBoxForDays.getValue();
        uValue = windData[day][windVal];
    }

    private void readPowerValue() {
        qValue = 4921.3;//TODO: SO2 and NO2
    }
}