package ru.tripcost.model.booking;

import ru.tripcost.model.domain.CostItem;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripOption;
import ru.tripcost.model.pricing.CostBreakdown;

import java.math.BigDecimal;

/**
 * Заказ на бронирование: всё, что нужно сервисам-участникам саги.
 */
public record BookingOrder(int requestId, String route, int passengers, int nights,
                           BigDecimal ticketsCost, BigDecimal hotelCost, BigDecimal insuranceCost,
                           BigDecimal total) {

    public static BookingOrder of(int requestId, Trip trip, CostBreakdown cost) {
        BigDecimal insurance = trip.getOptions().contains(TripOption.INSURANCE)
                ? find(cost.items(), "Страховка") : BigDecimal.ZERO;
        return new BookingOrder(requestId, trip.getOrigin() + " → " + trip.getDestination(),
                trip.getPassengers(), trip.getNights(),
                cost.transport().subtract(insurance), cost.accommodation(), insurance, cost.total());
    }

    public boolean needsHotel() {
        return nights > 0;
    }

    public boolean needsInsurance() {
        return insuranceCost.signum() > 0;
    }

    /** Сумма статьи дерева расходов, название которой начинается с {@code prefix}. */
    private static BigDecimal find(CostItem item, String prefix) {
        if (item.children().isEmpty()) {
            return item.name().startsWith(prefix) ? item.amount() : BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (CostItem child : item.children()) {
            sum = sum.add(find(child, prefix));
        }
        return sum;
    }
}
