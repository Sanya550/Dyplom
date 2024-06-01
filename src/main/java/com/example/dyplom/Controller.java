package com.example.dyplom;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class Controller implements Initializable {

    /**
     * Середньодобова vs Максимально разова ГДК
     */
    private static final double MAX_GDK_SO2 = 0.5;
    private static final double MAX_GDK_NO2 = 0.2;

    //berliand page 43 stan = 3;
    private static double alfaDifusion;
    private static double aDifusion;
    private static double betaDifusion;
    private static double bDifusion;

    //кількість опадів за місяць
    private static double quantityOfPrecipation;

    //швидкість вітру
    public static double uValue;

    public static int vectorOfWind;

    public static Map<String, List<Double>> dataMap = new LinkedHashMap<>();
    public static Map<Integer, String> mapTripilska = new LinkedHashMap<>();

    @FXML
    private ComboBox<String> comboBoxForDays;

    @FXML
    private ComboBox<Integer> comboBoxForH;

    @FXML
    private ComboBox<Integer> comboBoxTripilska;

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

    @FXML
    private Circle circle1;

    @FXML
    private Circle circle2;

    @FXML
    private Circle circle3;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        circle1.setFill(Color.GREEN);
        circle2.setFill(Color.RED);
        circle3.setFill(Color.BLUE);
        var optionsForH = FXCollections.observableArrayList(50, 100, 200);
        comboBoxForH.setItems(optionsForH);
        comboBoxForH.setValue(optionsForH.get(1));

        var optionsForChemistry = FXCollections.observableArrayList("SO2", "NO2");
        comboBoxChemistryElements.setItems(optionsForChemistry);
        comboBoxChemistryElements.setValue(optionsForChemistry.get(0));
    }

    @FXML
    public void tripilska() {
        var val = comboBoxTripilska.getValue();
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());

        drawHeatMapWithRadius(val);

        var path = mapTripilska.get(val);
        Image image = new Image(path);
        gc.setGlobalAlpha(0.5);
        gc.drawImage(image, 0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
    }

    @FXML
    private void drawHeatmap() {
        int r = parseInt(radiusForHeatMap.getText());
        drawHeatMapWithRadius(r);
    }

    private void drawHeatMapWithRadius( int r) {
        vectorOfWind = (int) Math.round(dataMap.get(comboBoxForDays.getValue()).get(0));
        uValue = dataMap.get(comboBoxForDays.getValue()).get(1);
        double z = parseDouble(rubZ.getText());
        double hEf = comboBoxForH.getValue();
        double q = getPowerValue();
        drawHeatMap(vectorOfWind, r, q, uValue, z, hEf);
    }

    @FXML
    public void findConcentationInDot() {
        uValue = dataMap.get(comboBoxForDays.getValue()).get(1);
        double x = parseDouble(rubX.getText());
        double y = parseDouble(rubY.getText());
        double z = parseDouble(rubZ.getText());
        double hEf = comboBoxForH.getValue();
        double q = getPowerValue();
        double c = getCValueForCoordinate1(new Coordinate(x, y, z), q, uValue, hEf);
        String textGdk = gdkValueIsAcceptable(c) ? "\nРівень концентрації є прийнятним!" : "\nРівень концентрації перевищує допустиму норму!";
        JOptionPane.showMessageDialog(null, "Концентрація дорівнює " + String.format("%.1E", c) + textGdk, "Details", JOptionPane.INFORMATION_MESSAGE);
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
        quantityOfPrecipation = dataMap.get(comboBoxForDays.getValue()).get(2);
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
            qValue = 3820.8;
        } else {
            qValue = 33595.3;
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


            case 3:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        double value = heatMapData[x][y];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                    }
                }
                break;

            case 5:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        double value = heatMapData[y][x];
                        Color color = getColorForConcentration(value);
                        gc.setFill(color);
                        gc.fillRect(x, y, 5, 5);
                    }
                }
                break;


            case 7:
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

            case 4:
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
                    for (int y = x; y > width / 2; y--) {
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
                gc.fillRect(width / 2, width / 2, 5, 5);
                break;

            case 8:
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        double value = 0d;
                        Color color6 = getColorForConcentration(value);
                        gc.setFill(color6);
                        gc.fillRect(i, j, 5, 5);
                    }
                }

                var angle6 = radius;
                heatMapData = divideOnTwoMatrix(generateHeatMapData(width, (int) angle6, q, u, coordinateH, startH));

                int y6 = heatMapData[0].length - 1;
                int x6 = 0;
                for (int x = width / 2 - 1; x >= 0; x--) {
                    for (int y = x; y < width / 2; y++) {
                        double value6 = heatMapData[x6][y6];
                        Color color6 = getColorForConcentration(value6);
                        gc.setFill(color6);
                        gc.fillRect(x, y, 5, 5);
                        y6--;
                    }

                    y6 = heatMapData[0].length - 1;
                    for (int y = x; y >= 0; y--) {
                        double value = heatMapData[x6][y6];
                        Color color6 = getColorForConcentration(value);
                        gc.setFill(color6);
                        gc.fillRect(x, y, 5, 5);
                        y6--;

                    }

                    y6 = heatMapData[0].length - 1;
                    x6++;
                }

                Color color6 = getColorForConcentration(0d);
                gc.setFill(color6);
                gc.fillRect(width / 2, width / 2, 5, 5);
                break;

            case 6:
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        double value = 0d;
                        Color color2 = getColorForConcentration(value);
                        gc.setFill(color2);
                        gc.fillRect(i, j, 5, 5);
                    }
                }

                var angle2 = radius;
                heatMapData = divideOnTwoMatrix(generateHeatMapData(width, (int) angle2, q, u, coordinateH, startH));

                int y2 = heatMapData[0].length - 1;
                int x2 = 0;
                for (int x = width / 2 - 1; x >= 0; x--) {
                    for (int y = x; y < width / 2; y++) {
                        double value2 = heatMapData[x2][y2];
                        Color color2 = getColorForConcentration(value2);
                        gc.setFill(color2);
                        gc.fillRect(x, height - y, 5, 5);
                        y2--;
                    }

                    y2 = heatMapData[0].length - 1;
                    for (int y = x; y >= 0; y--) {
                        double value = heatMapData[x2][y2];
                        Color color2 = getColorForConcentration(value);
                        gc.setFill(color2);
                        gc.fillRect(x, height - y, 5, 5);
                        y2--;

                    }

                    y2 = heatMapData[0].length - 1;
                    x2++;
                }

                Color color2 = getColorForConcentration(0d);
                gc.setFill(color2);
                gc.fillRect(width / 2, width / 2, 5, 5);
                break;


            case 2:
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        double value = 0d;
                        Color color3 = getColorForConcentration(value);
                        gc.setFill(color3);
                        gc.fillRect(i, j, 5, 5);
                    }
                }

                var angle3 = radius;
                heatMapData = divideOnTwoMatrix(generateHeatMapData(width, (int) angle3, q, u, coordinateH, startH));

                int y3 = heatMapData[0].length - 1;
                int x3 = 0;
                for (int x = width / 2 - 1; x >= 0; x--) {
                    for (int y = x; y < width / 2; y++) {
                        double value6 = heatMapData[x3][y3];
                        Color color3 = getColorForConcentration(value6);
                        gc.setFill(color3);
                        gc.fillRect(width - x, y, 5, 5);
                        y3--;
                    }

                    y3 = heatMapData[0].length - 1;
                    for (int y = x; y >= 0; y--) {
                        double value = heatMapData[x3][y3];
                        Color color3 = getColorForConcentration(value);
                        gc.setFill(color3);
                        gc.fillRect(width - x, y, 5, 5);
                        y3--;

                    }

                    y3 = heatMapData[0].length - 1;
                    x3++;
                }

                Color color3 = getColorForConcentration(0d);
                gc.setFill(color3);
                gc.fillRect(width / 2, width / 2, 5, 5);
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
                } catch (ArrayIndexOutOfBoundsException e) {

                }
            }
            counterWidth = 0;
        }
        return data;
    }

    private Color getColorForConcentration(double normalization) {
        return gdkValueIsAcceptable(normalization) ? Color.BLUE.interpolate(Color.RED, normalization) : Color.GREEN;
    }

    private boolean gdkValueIsAcceptable(double c) {
        var gdk = 0d;
        if (comboBoxChemistryElements.getValue().contains("NO2")) {
            gdk = MAX_GDK_NO2;
        } else {
            gdk = MAX_GDK_SO2;
        }
        return c / gdk < 1d;
    }

    @FXML
    public void windData() {
        dataMap.clear();
        var fileopen = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileopen.setDialogTitle("Виберіть текстовий файл з даними");
        fileopen.showDialog(null, "OK");
        File file = fileopen.getSelectedFile();
        String s = file.getPath();
        List<String> listString = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Path.of(s))) {
            listString = br.lines().collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        listString = listString.stream().map(v -> v.replaceAll("\\s+", " ").trim()).collect(Collectors.toList());
        listString.remove(0);
        ObservableList<String> days = FXCollections.observableArrayList();
        for (int i = 0; i < listString.size(); i++) {
            var values = Arrays.stream(listString.get(i).trim().split("\\s+")).collect(Collectors.toList());
            var date = values.get(0).trim();
            values.remove(0);
            var otherData = values.stream().map(Double::parseDouble).collect(Collectors.toList());
            dataMap.put(date, otherData);
            days.add(date);
        }
        comboBoxForDays.setItems(days);
        comboBoxForDays.setValue(days.get(0));
    }

    @FXML
    public void readTripilska() {
        mapTripilska.clear();
        var fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Виберіть папку, в якій знаходяться фотографії карт");

        int returnValue = fileChooser.showDialog(null, "OK");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            String folderPath = selectedFolder.getAbsolutePath();

            List<String> pngFiles = new ArrayList<>();
            File[] files = selectedFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    pngFiles.add(file.getName());
                }
            }

            var intPng = pngFiles.stream().map(v -> Integer.parseInt(v.split("\\.")[0])).collect(Collectors.toList());
            intPng.sort(Comparator.naturalOrder());
            ObservableList<Integer> observableListData = FXCollections.observableArrayList(intPng);
            comboBoxTripilska.setItems(observableListData);
            comboBoxTripilska.setValue(observableListData.get(0));

            for (int i = 0; i < intPng.size(); i++) {
                mapTripilska.put(intPng.get(i), String.format("%s\\%d.png", folderPath, intPng.get(i)));
            }

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