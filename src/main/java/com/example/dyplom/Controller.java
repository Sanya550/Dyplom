package com.example.dyplom;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
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

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class Controller implements Initializable {

    /**
     * data вітру(4 сторони) + кількість опадів за місяць
     * гдк
     * функціонал?
     */

    //berliand page 43 stan = 3;
    private static double alfaDifusion;
    private static double aDifusion;
    private static double betaDifusion;
    private static double bDifusion;

    //кількість опадів за місяць
    private static final double quantityOfPrecipation = 5d;

    //швидкість вітру
    public static double uValue = 15.5;

    public static double[][] windData;

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

    //Координати
    @FXML
    private TextField rubX;
    @FXML
    private TextField rubY;
    @FXML
    private TextField rubZ;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var optionsForH = FXCollections.observableArrayList(50, 100, 200);
        comboBoxForH.setItems(optionsForH);
        comboBoxForH.setValue(optionsForH.get(1));

        var optionsForChemistry = FXCollections.observableArrayList("SO2", "NO2");
        comboBoxChemistryElements.setItems(optionsForChemistry);
        comboBoxChemistryElements.setValue(optionsForChemistry.get(0));
    }

    @FXML
    private void drawHeatmap() {
        double z = parseDouble(rubZ.getText());
        double hEf = comboBoxForH.getValue();
        double q = getPowerValue();
        int r = parseInt(radiusForHeatMap.getText());
        drawHeatMap(5, r, q, uValue, z, hEf);
    }

    @FXML
    public void findConcentationInDot() {
        double x = parseDouble(rubX.getText());
        double y = parseDouble(rubY.getText());
        double z = parseDouble(rubZ.getText());
        double hEf = comboBoxForH.getValue();
        double q = getPowerValue();
        System.out.println(getCValueForCoordinate1(new Coordinate(x, y, z), q, uValue, hEf));
    }

    private double getCValueForCoordinate1(Coordinate coordinate, double q, double u, double h) {
        var distance = Math.sqrt(Math.pow(coordinate.getX(), 2) + Math.pow(coordinate.getY(), 2) + Math.pow(coordinate.getZ() - h, 2));
        var fOp = getFunctionRozpodilByKvantil(getKvantilForKoefOfOpad(), distance, u, h);
        var fChemistry = getFunctionRozpodilByKvantil(getKvantilForKoefOfChemistry(), distance, u, h);

        double sigmaY = getDifusionForY(distance);
        double sigmaZ = getDifusionForZ(distance);
        double concentration = (q / (2 * Math.PI * sigmaY * sigmaZ * u)) * Math.exp(-0.5 * Math.pow(coordinate.getY(), 2) / (sigmaY * sigmaY))
                * (Math.exp(-0.5 * Math.pow(coordinate.getZ() - h, 2) / (sigmaZ * sigmaZ)) + Math.exp(-0.5 * Math.pow(coordinate.getZ() + h, 2) / (sigmaZ * sigmaZ)))
                * fOp * fChemistry;
        return concentration;
    }

    //горизонтальна дифузія
    private double getDifusionForY(double distance) {
        var val = comboBoxForH.getValue();
        if (val == 50) {
            alfaDifusion = 0.89;
            aDifusion = 0.14;
        } else if (val == 100) {
            alfaDifusion = 0.78;
            aDifusion = 0.32;
        } else if (val == 200) {
            alfaDifusion = 0.76;
            aDifusion = 0.37;
        } else {
            throw new RuntimeException("ERROR!!!");
        }
        return aDifusion * Math.pow(distance, alfaDifusion);
    }

    //вертикальна дифузія
    private double getDifusionForZ(double distance) {
        var val = comboBoxForH.getValue();
        if (val == 50) {
            betaDifusion = 0.65;
            bDifusion = 0.73;
        } else if (val == 100) {
            betaDifusion = 0.78;
            bDifusion = 0.22;
        } else if (val == 200) {
            betaDifusion = 2.44;
            bDifusion = 0.34;
        } else {
            throw new RuntimeException("ERROR!!!");
        }
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

    private double getFunctionRozpodilByKvantil(double kvantil, double distance, double u, double hEf) {
        return Math.pow(Math.E, -kvantil * distance / u / hEf);
    }

    private double getPowerValue() {
        double qValue;
        if (comboBoxChemistryElements.getValue().contains("NO2")) {
            qValue = 5330.1;
        } else {
            qValue = 4921.3;
        }
        return qValue;
    }

    private void drawHeatMap(int vector, int radius, double q, double u, double coordinateH, double startH) {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        int width = (int) heatmapCanvas.getWidth();
        int height = (int) heatmapCanvas.getHeight();

        double[][] heatMapData = generateHeatMapData(width, radius, q, u, coordinateH, startH);

        switch (vector) {
            case 1:
                int tempY = 0;
                for (int y = height - 1; y >= 0; y--) {
                    int tempX = 0;
                    for (int x = width - 1; x >= 0; x--) {
                        double value = heatMapData[y][x];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(tempX, tempY, 5, 5);
                        tempX++;
                    }
                    tempX = 0;
                    tempY++;
                }
                break;


            case 2:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        double value = heatMapData[x][y];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                    }
                }
                break;

            case 3:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        double value = heatMapData[y][x];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                    }
                }
                break;


            case 4:
                int tempY4 = 0;
                for (int y = height - 1; y >= 0; y--) {
                    int tempX4 = 0;
                    for (int x = width - 1; x >= 0; x--) {
                        double value = heatMapData[x][y];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(tempX4, tempY4, 5, 5);
                        tempX4++;
                    }
                    tempX4 = 0;
                    tempY4++;
                }
                break;

            case 5:
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        double value = 0d;
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(i, j, 5, 5);
                    }
                }

                var angle = radius;
                heatMapData = divideOnTwoMatrix(generateHeatMapData(width, (int) angle, q, u, coordinateH, startH));

                int y1 = heatMapData[0].length - 1;
                int x1 = 0;
                for (int x = width / 2; x < width; x++) {
                    for (int y = x; y < width; y++) {
                        double value = heatMapData[x1][y1];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                        y1--;
                    }

                    y1 = heatMapData[0].length - 1;
                    for (int y = x; y > width/2; y--) {
                        double value = heatMapData[x1][y1];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                        y1--;
                    }

                    y1 = heatMapData[0].length - 1;
                    x1++;
                }

                Color color = getColorForConcentration(0d);
                gc.setFill(color);
                gc.fillRect(width/2, width/2, 5, 5);
                break;

            case 6:
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        double value = 0d;
                        Color color1 = getColorForConcentration(value);
                        gc.setFill(color1);
                        gc.fillRect(i, j, 5, 5);
                    }
                }

                var angle1 = radius;
                heatMapData = divideOnTwoMatrix(generateHeatMapData(width, (int) angle1, q, u, coordinateH, startH));

                int y6 = heatMapData[0].length - 1;
                int x6 = 0;
                for (int x = 0; x < width / 2; x++) {
                    for (int y = x; y < width/2; y++) {
                        double value = heatMapData[x6][y6];
                        Color color6 = getColorForConcentration(value);
                        gc.setFill(color6);
                        gc.fillRect(x, y, 5, 5);
                        y6--;
                    }

//                    y1 = heatMapData[0].length - 1;
//                    for (int y = x; y < width/2; y++) {
//                        double value = heatMapData[x6][y6];
//                        Color color6 = getColorForConcentration(value);
//                        gc.setFill(color6);
//                        gc.fillRect(x, y, 5, 5);
//                        y6--;
//                    }

                    y6 = heatMapData[0].length - 1;
                    x6++;
                }

                Color color6 = getColorForConcentration(0d);
                gc.setFill(color6);
                gc.fillRect(width/2, width/2, 5, 5);
                break;
        }
    }

//        Color center = Color.BLACK;
//        gc.setFill(center);
//        gc.fillRect(width / 2, height / 2, 10, 10);


    private double[][] generateHeatMapData(int width, int radius, double q, double u, double coordinateH, double startH) {
        double step = (double) radius * 2.0 / (double) width;
        double[][] data = new double[width][width];
        int counterWidth = 0;
        int counterHeight = width / 2 - 1;
        for (double i = 0; i < radius; i += step) {
            counterHeight++;
            for (double j = -radius; j < radius; j += step) {
                try {
                    if (i == 0 && j == 0) {
                        data[counterHeight][counterWidth] = 0d;
                    } else {
                        double val = getCValueForCoordinate1(new Coordinate(i, j, coordinateH), q, u, startH);
                        data[counterHeight][counterWidth] = val;
                    }
                    counterWidth++;
                } catch (ArrayIndexOutOfBoundsException e){

                }
            }
            counterWidth = 0;
        }
        return data;
    }

    private Color getColorForConcentration(double normalization) {
        return Color.BLUE.interpolate(Color.RED, normalization);
    }


    /**
     * Частина яка нижче не використовується
     */

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

    private void readWindValue() {
//        var windVal = parseInt(String.valueOf(comboBoxForVector.getValue().charAt(0))) - 1;
//        var day = comboBoxForDays.getValue();
//        uValue = windData[day][windVal];
    }

    private double[][] divideOnTwoMatrix(double[][] matrix) {
        double[][] res = new double[matrix.length / 2][matrix.length / 2];
        int c = 0;
        for (int i = matrix.length / 2; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length / 2; j++) {
                res[c][j] = matrix[i][j];
            }
            c++;
        }
        return res;
    }
}