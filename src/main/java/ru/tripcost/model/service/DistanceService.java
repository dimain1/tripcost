package ru.tripcost.model.service;

import java.util.List;

/**
 * Сервис расстояний в том виде, который нужен приложению (Target для паттерна «Адаптер»):
 * русские названия городов на входе, километры на выходе.
 */
public interface DistanceService {

    /**
     * Расстояние между городами.
     *
     * @throws IllegalArgumentException если город неизвестен
     */
    RouteDistance distance(String from, String to);

    /** Города, для которых сервис знает расстояния. */
    List<String> knownCities();
}
