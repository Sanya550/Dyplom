package com.example.dyplom;

import java.util.List;

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
}
