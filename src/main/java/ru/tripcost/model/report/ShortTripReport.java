package ru.tripcost.model.report;

import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

import java.time.LocalDateTime;
import java.util.List;

/** Краткий отчёт: маршрут, итог и доля на пассажира. */
public class ShortTripReport extends TripReport {

    public ShortTripReport(ReportFormat format, Trip trip, CostBreakdown cost, LocalDateTime createdAt) {
        super(format, trip, cost, createdAt);
    }

    @Override
    protected String title() {
        return "Стоимость поездки";
    }

    @Override
    protected String body() {
        return format.section(null, List.of(
                ReportLine.of("Маршрут", route()),
                ReportLine.of("Транспорт", trip.getTransport().getDisplayName()),
                ReportLine.strong("Итого", money(cost.total())),
                ReportLine.of("На одного пассажира", money(cost.perPassenger()))));
    }
}
