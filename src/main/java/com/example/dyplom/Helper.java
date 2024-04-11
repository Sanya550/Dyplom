package com.example.dyplom;

import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

public class Helper {
    public static double[][] convertToDoubleArray(List<String> stringList) {
        int rows = stringList.size();
        int cols = stringList.get(0).split(" ").length;
        double[][] doubleArray = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            String[] values = stringList.get(i).split(" ");
            for (int j = 0; j < cols; j++) {
                doubleArray[i][j] = Double.parseDouble(values[j]);
            }
        }

        return doubleArray;
    }

    public static double[][] divideByMax(double[][] array) {
        // Находим максимальное значение в массиве
        double max = array[0][0];
        for (double[] row : array) {
            for (double value : row) {
                if (value > max) {
                    max = value;
                }
            }
        }

        // Создаем новый массив и делим каждый элемент на максимальное значение
        double[][] newArray = new double[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                newArray[i][j] = array[i][j] / max;
            }
        }

        return newArray;
    }

    public static double[][] normalizeData(double[][] data) {
        double[][] normalizedData = new double[data.length][data[0].length];

        for (int i = 0; i < data.length; i++) {
            double[] row = data[i];
            double min = DoubleStream.of(row).min().orElseThrow();
            double max = DoubleStream.of(row).max().orElseThrow();

            for (int j = 0; j < row.length; j++) {
                normalizedData[i][j] = (Math.log(row[j] - min + 1) / Math.log(max - min + 1));
            }
        }

        return normalizedData;
    }

    public static double[][] getRandomData(int rows, int cols) {
        double[][] randomData = new double[rows][cols];
        Random random = new Random();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                randomData[i][j] = random.nextDouble();
            }
        }

        return randomData;
    }
}
