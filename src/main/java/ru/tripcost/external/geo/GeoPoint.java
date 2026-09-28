package ru.tripcost.external.geo;

/**
 * Точка на карте в формате сторонней геобиблиотеки: широта и долгота в градусах.
 */
public record GeoPoint(double latitude, double longitude) {
}
