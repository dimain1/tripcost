package ru.tripcost.model.journey;

import java.math.BigDecimal;

/**
 * Расчёт одного участка маршрута.
 *
 * @param number номер участка (с 1)
 * @param total  стоимость участка или {@code null}, если участок рассчитать нельзя
 * @param note   предупреждение или причина ошибки
 */
public record LegQuote(int number, Leg leg, double distanceKm, BigDecimal total, String note) {

    public boolean isPriced() {
        return total != null;
    }
}
