package ru.tripcost.model.domain;

/**
 * Готовые шаблоны поездок, которые умеет собирать {@link TripDirector}.
 */
public enum TripPreset {

    WEEKEND_BY_CAR("Выходные в Кисловодске на машине"),
    BUSINESS_BY_TRAIN("Командировка в Москву на поезде"),
    VACATION_BY_PLANE("Отпуск в Санкт-Петербурге на самолёте"),
    CITY_TAXI("Такси до аэропорта");

    private final String displayName;

    TripPreset(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
