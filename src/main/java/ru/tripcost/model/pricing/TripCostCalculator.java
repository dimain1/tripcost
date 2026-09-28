package ru.tripcost.model.pricing;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.CostGroup;
import ru.tripcost.model.domain.CostItem;
import ru.tripcost.model.domain.CostLeaf;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.transport.Transport;
import ru.tripcost.model.transport.TransportFactories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Расчёт стоимости поездки.
 *
 * <p>Стоимость проезда считает объект {@link Transport}, созданный фабрикой для вида
 * транспорта и обёрнутый декораторами выбранных услуг. Все расходы собираются
 * в дерево статей {@link CostGroup}. Валюта берётся из {@link AppConfig}.</p>
 */
public class TripCostCalculator {

    private final AppConfig config = AppConfig.getInstance();

    public CostBreakdown calculate(Trip trip) {
        // транспорт создаёт фабрика (Фабричный метод) и оборачивает декораторами услуг (Декоратор)
        Transport transport = TransportFactories.forType(trip.getTransport()).createFor(trip);

        // расходы собираются в дерево статей (Компоновщик)
        List<CostItem> items = new ArrayList<>();
        CostGroup travel = transport.breakdown(trip);
        items.add(travel);
        if (trip.getTollCost().signum() > 0) {
            items.add(new CostLeaf("Платные дороги",
                    trip.getTollCost().multiply(BigDecimal.valueOf(trip.getLegs()))));
        }
        if (trip.getNights() > 0) {
            items.add(new CostLeaf("Проживание: " + trip.getNights() + " ноч.",
                    trip.getPricePerNight().multiply(BigDecimal.valueOf(trip.getNights()))));
        }
        CostGroup all = new CostGroup("Все расходы", items);

        BigDecimal transportCost = travel.amount();
        BigDecimal tolls = trip.getTollCost().multiply(BigDecimal.valueOf(trip.getLegs()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal accommodation = trip.getPricePerNight().multiply(BigDecimal.valueOf(trip.getNights()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = all.amount();
        BigDecimal perPassenger = total.divide(BigDecimal.valueOf(trip.getPassengers()), 2, RoundingMode.HALF_UP);

        return new CostBreakdown(transportCost, tolls, accommodation, total, perPassenger,
                config.getCurrency(), all);
    }
}
