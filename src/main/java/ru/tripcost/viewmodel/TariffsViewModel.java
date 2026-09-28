package ru.tripcost.viewmodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.transport.Transport;
import ru.tripcost.model.transport.TransportFactories;

/**
 * ViewModel вкладки «Тарифы». Описание тарифа даёт сам транспорт, созданный
 * фабрикой; цены транспорт берёт из единственного экземпляра AppConfig.
 */
public class TariffsViewModel {

    private final ObservableList<TariffRow> rows = FXCollections.observableArrayList();

    public TariffsViewModel() {
        // каждая строка описывает транспорт, созданный своей фабрикой
        for (TransportType type : TransportType.values()) {
            Transport transport = TransportFactories.forType(type).createTransport();
            rows.add(new TariffRow(type.getDisplayName(), transport.describeBaseFee(), transport.describePerKm()));
        }
    }

    public ObservableList<TariffRow> getRows() {
        return rows;
    }

    /** Строка таблицы тарифов. */
    public record TariffRow(String transport, String baseFee, String perKm) {
    }
}
