package com.example.dyplom;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

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

    @FXML
    private ComboBox<String> comboBoxChemistryElements;

    @FXML
    private Canvas heatmapCanvas;

    @FXML
    private TextField radiusForHeatMap;


    @FXML
    private TextField rubX;
    @FXML
    private TextField rubY;
    @FXML
    private TextField rubZ;


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

        var optionsForChemistry = FXCollections.observableArrayList("SO2", "NO2");
        comboBoxChemistryElements.setItems(optionsForChemistry);
        comboBoxChemistryElements.setValue(optionsForChemistry.get(0));
    }

    @FXML
    private void drawHeatmap() {
        drawHeatMap(1, 100, 5330, 6.5, 100, 100);
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

    @FXML
    public void rubish() {
        generateHeatMapDataFor1Vector(200, 100, 5330, 3d, 100d, 100d);
        double x = Double.parseDouble(rubX.getText());
        double y = Double.parseDouble(rubY.getText());
        double z = Double.parseDouble(rubZ.getText());
        System.out.println(getCValueForCoordinate1(new Coordinate(x, y, z), 5330, 3d, 100d));
    }

    private double getCValueForCoordinate1(Coordinate coordinate, double q, double u, double h) {
        var distance = Math.sqrt(Math.pow(coordinate.getX(), 2) + Math.pow(coordinate.getY(), 2) + Math.pow(coordinate.getZ() - h, 2));
        var fOp = getFunctionRozpodilByKvantil(getKvantilForKoefOfOpad(), distance, u);
        var fChemistry = getFunctionRozpodilByKvantil(getKvantilForKoefOfChemistry(), distance, u);

        double sigmaY = getDifusionForY(distance);
        double sigmaZ = getDifusionForZ(distance);
        double concentration = (q / (2 * Math.PI * sigmaY * sigmaZ * u)) * Math.exp(-0.5 * Math.pow(coordinate.getY(), 2) / (sigmaY * sigmaY))
                * (Math.exp(-0.5 * Math.pow(coordinate.getZ() - h, 2) / (sigmaZ * sigmaZ)) + Math.exp(-0.5 * Math.pow(coordinate.getZ() + h, 2) / (sigmaZ * sigmaZ)))
                * fOp * fChemistry;
        return concentration;
    }

    //С - концентрация в некоторой точке с координатами x; y; z; q – мощность выброса, г/с;
    //Н – высота виртуального источника; u - средняя скорость ветра, м/с;
    private double getCValueForCoordinate(Coordinate coordinate, double q, double u, double h) {
        double distance = Double.parseDouble(distanceField.getText());
        double sigmaY = getDifusionForY(distance);
        double sigmaZ = getDifusionForZ(distance);

        var fOp = getFunctionRozpodilByKvantil(getKvantilForKoefOfOpad(), distance, u);
        var fChemistry = getFunctionRozpodilByKvantil(getKvantilForKoefOfChemistry(), distance, u);

        double concentration = (q / (2 * Math.PI * sigmaY * sigmaZ * u));
        if (coordinate.getY() != 0) {
            concentration *= Math.exp(-0.5 * Math.pow(coordinate.getY(), 2) / (sigmaY * sigmaY));
        }
        if (coordinate.getZ() != h) {
            concentration *= (Math.exp(-0.5 * Math.pow(coordinate.getZ() - h, 2) / (sigmaZ * sigmaZ)) + Math.exp(-0.5 * Math.pow(coordinate.getZ() + h, 2) / (sigmaZ * sigmaZ)));
        }
        return concentration * fOp * fChemistry;
    }

    //горизонтальна дифузія
    private double getDifusionForY(double distance) {
        return aDifusion * Math.pow(distance, alfaDifusion);
    }

    //вертикальна дифузія
    private double getDifusionForZ(double distance) {
        return bDifusion * Math.pow(distance, betaDifusion);
    }

    private double getKvantilForKoefOfChemistry() {
        if (comboBoxChemistryElements.getValue().contains("NO2")) {
            return 3 * 0.000001;
        } else {
            return 14 * 0.000001;
        }
    }

    private double getKvantilForKoefOfOpad() {
        var i = quantityOfPrecipation / (24 * 30);
        var kvantilOp = i <= 0.2 ? 11.8 * Math.pow(i, 0.9) * Math.pow(Math.E, -2 * i) : 7 * Math.pow(i - 0.1, 0.575);
        kvantilOp *= 0.00001;
        return kvantilOp;
    }

    private double getFunctionRozpodilByKvantil(double kvantil, double distance, double u) {
        return Math.pow(Math.E, -kvantil * distance / u);
    }

    private void readCoordinate() {
        double distance = Double.parseDouble(distanceField.getText());
        double z = comboBoxForH.getValue();
        double x, y;
        if (comboBoxForVector.getValue().startsWith("1") || comboBoxForVector.getValue().startsWith("5")) {
            y = distance;
            x = 1d;//todo
        } else if (comboBoxForVector.getValue().startsWith("3") || comboBoxForVector.getValue().startsWith("7")) {
            y = 1d;//todo
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
        if (comboBoxChemistryElements.getValue().contains("NO2")) {
            qValue = 5330.1;
        } else {
            qValue = 4921.3;
        }
    }

    private Color getColorForConcentration(double normalization) {
        // Простая интерполяция от синего (низкая концентрация) к красному (высокая концентрация)
        return Color.BLUE.interpolate(Color.RED, normalization);
    }

    private void drawHeatMap(int vector, int radius, double q, double u, double coordinateH, double startH) {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        int width = (int) heatmapCanvas.getWidth();
        int height = (int) heatmapCanvas.getHeight();

        // Генерация данных для тепловой карты
//        double[][] heatMapData = generateHeatMapDataFor1Vector(width, radius, q,  u, coordinateH, startH);
//        heatMapData = Helper.divideByMax(heatMapData);
        double[][] heatMapData = Helper.getRandomData(width,width);

        switch (vector) {
            case 1:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        double value = heatMapData[x][y];// Получаем значение от 0.0 до 1.0
                        if (x <= width/2.0){
                            value = 0.0;
                        }
                        Color color = getColorForConcentration(value); // Получаем цвет для значения
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5); // Рисуем пиксель
                    }
                }
        }

        Color center = Color.BLACK; // Получаем цвет для значения
        gc.setFill(center);
        gc.fillRect(width / 2, height / 2, 5, 5);
    }

    private double[][] generateHeatMapDataFor1Vector(int width, int radius, double q, double u, double coordinateH, double startH) {
        double step = (double) radius * 2.0 / (double) width;
        double[][] data = new double[width][width];
        // Заполните data реальными значениями
        int counterWidth = 0;
        int counterHeight = -1;
        for (double i = 0; i < radius; i += step) {
            counterHeight++;
            for (double j = -radius; j < radius; j += step) {
                if (i == 0 && j == 0) {
                    data[counterHeight][counterWidth] = 0d;
                } else {
                    data[counterHeight][counterWidth] = getCValueForCoordinate1(new Coordinate(i, j, coordinateH), q, u, startH);
                }
                counterWidth++;
            }
            counterWidth = 0;
        }
        return data;
    }
}