package com.example.dyplom;

import java.util.Scanner;

public class PollutionCalculator {

    private static final double WIND_SPEED = 5.0; // Скорость ветра в м/с
    private static final double Q = 1000.0; // Скорость выброса загрязняющего вещества в единицах массы/время

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Ввод координат источника и точки измерения
        System.out.println("Введите координаты источника выброса (x, y, z): ");
        double sourceX = scanner.nextDouble();
        double sourceY = scanner.nextDouble();
        double sourceZ = scanner.nextDouble();

        System.out.println("Введите координаты точки измерения (x, y, z): ");
        double pointX = scanner.nextDouble();
        double pointY = scanner.nextDouble();
        double pointZ = scanner.nextDouble();

        // Расчет концентрации
        double concentration = calculateConcentration(sourceX, sourceY, sourceZ, pointX, pointY, pointZ);
        System.out.println("Концентрация загрязняющего вещества в заданной точке: " + concentration);
    }

    private static double calculateConcentration(double sourceX, double sourceY, double sourceZ, double pointX, double pointY, double pointZ) {
        double a = 0.32; // Коэффициент a для горизонтальной дисперсии
        double a1 = 0.78; // Показатель степени a1 для горизонтальной дисперсии
        double b = 0.22; // Коэффициент b для вертикальной дисперсии
        double b1 = 0.78; // Показатель степени b1 для вертикальной дисперсии

        double distance = Math.sqrt(Math.pow(pointX - sourceX, 2) + Math.pow(pointY - sourceY, 2));

        double sigmaY = a * Math.pow(distance, a1);
        double sigmaZ = b * Math.pow(distance, b1);

        double H = sourceZ; // Эффективная высота выброса

        double concentration = (Q / (2 * Math.PI * sigmaY * sigmaZ * WIND_SPEED))
                * Math.exp(-0.5 * Math.pow(pointY - sourceY, 2) / (sigmaY * sigmaY))
                * (Math.exp(-0.5 * Math.pow(pointZ - H, 2) / (sigmaZ * sigmaZ))
                + Math.exp(-0.5 * Math.pow(pointZ + H, 2) / (sigmaZ * sigmaZ)));

        return concentration;
    }
}
