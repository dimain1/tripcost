package ru.tripcost.model.domain;

import java.math.BigDecimal;

/**
 * Предложение по направлению - контекст приспособленца.
 *
 * <p>Хранит внешнее состояние (вид транспорта, расстояние, стоимость) и ссылки
 * на общие объекты городов {@link City}. Таких объектов может быть очень много,
 * а городов - столько, сколько их в справочнике.</p>
 *
 * @param total  итог поездки или {@code null}, если вариант недоступен
 * @param note   причина недоступности
 * @param approval кто согласует поездку (цепочка согласования) или пустая строка
 */
public record RouteQuote(City from, City to, TransportType transport, double distanceKm,
                         BigDecimal total, BigDecimal perPassenger, String note, String approval) {

    public boolean isAvailable() {
        return total != null;
    }

    /** Подпись маршрута строит сам город-приспособленец по внешнему состоянию. */
    public String describe() {
        return from.routeTo(to, distanceKm);
    }
}
