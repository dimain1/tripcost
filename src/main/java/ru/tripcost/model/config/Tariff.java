package ru.tripcost.model.config;

import java.math.BigDecimal;

/**
 * Тариф вида транспорта: фиксированная часть за поездку в одну сторону
 * и цена за километр.
 *
 * @param baseFee фиксированная часть (посадка в такси, сбор аэропорта и т. п.)
 * @param perKm   цена за километр
 */
public record Tariff(BigDecimal baseFee, BigDecimal perKm) {
}
