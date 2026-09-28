package ru.tripcost.viewmodel;

/**
 * Вкладки главного окна в порядке их следования.
 */
public enum AppTab {
    CALCULATOR("Расчёт"),
    JOURNEY("Маршрут"),
    DIRECTIONS("Направления"),
    REQUESTS("Заявки"),
    TARIFFS("Тарифы");

    private final String title;

    AppTab(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
