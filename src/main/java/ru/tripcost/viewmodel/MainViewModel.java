package ru.tripcost.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import ru.tripcost.model.config.AppConfig;

/**
 * ViewModel главного окна: строка состояния и выбранная вкладка.
 * Создаёт посредника, который связывает модели представления вкладок.
 */
public class MainViewModel {

    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper();
    private final ObjectProperty<AppTab> selectedTab = new SimpleObjectProperty<>(AppTab.CALCULATOR);
    private PlannerMediator mediator;

    public MainViewModel() {
        AppConfig config = AppConfig.getInstance();
        status.set("Настройки загружены из app.properties · валюта: " + config.getCurrency()
                + " · топливо: " + config.getFuelPrice().stripTrailingZeros().toPlainString()
                + " " + config.getCurrency() + "/л");
    }

    /** Связывает вкладки через посредника (вызывается один раз при создании окна). */
    public void connect(TripCalculatorViewModel calculator, JourneyViewModel journey,
                        DirectionsViewModel directions, RequestsViewModel requests) {
        mediator = new TripPlannerMediator(this, calculator, journey, directions, requests);
    }

    void showStatus(String message) {
        status.set(message);
    }

    void selectTab(AppTab tab) {
        selectedTab.set(tab);
    }

    public ObjectProperty<AppTab> selectedTabProperty() {
        return selectedTab;
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }
}
