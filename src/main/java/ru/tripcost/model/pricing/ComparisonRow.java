package ru.tripcost.model.pricing;

import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.transport.TravelTime;

/**
 * Строка сравнения видов транспорта: стоимость и время в пути или причина,
 * по которой вариант недоступен.
 *
 * @param travelTime время в пути в одну сторону
 * @param legs       поездок в одну сторону (2 - туда и обратно)
 * @param passengers число пассажиров
 */
public record ComparisonRow(TransportType transport, CostBreakdown cost, String note,
                            TravelTime travelTime, int legs, int passengers) {

    public static ComparisonRow available(TransportType transport, CostBreakdown cost,
                                          TravelTime travelTime, int legs, int passengers) {
        return new ComparisonRow(transport, cost, "", travelTime, legs, passengers);
    }

    public static ComparisonRow unavailable(TransportType transport, String reason) {
        return new ComparisonRow(transport, null, reason, null, 0, 0);
    }

    public boolean isAvailable() {
        return cost != null;
    }

    /** Время в пути за всю поездку (с обратной дорогой), ч. */
    public double totalHours() {
        return travelTime == null ? 0 : travelTime.hours() * legs;
    }
}
