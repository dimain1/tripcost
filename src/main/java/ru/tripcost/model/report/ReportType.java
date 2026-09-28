package ru.tripcost.model.report;

import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

import java.time.LocalDateTime;

/** Вид отчёта, который выбирает пользователь. */
public enum ReportType {

    SHORT("Краткий"),
    DETAILED("Подробный");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    /** Создаёт отчёт и соединяет его с выбранным форматом. */
    public TripReport create(ReportFormat format, Trip trip, CostBreakdown cost) {
        LocalDateTime now = LocalDateTime.now();
        return switch (this) {
            case SHORT -> new ShortTripReport(format, trip, cost, now);
            case DETAILED -> new DetailedTripReport(format, trip, cost, now);
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
