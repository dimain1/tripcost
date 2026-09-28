package ru.tripcost.model.service;

import ru.tripcost.model.domain.TransportType;

/**
 * Расстояние между городами: по прямой и оценка по дорогам, км.
 */
public record RouteDistance(double straightKm, double roadKm) {

    /** Самолёт летит по прямой, остальной транспорт едет по дорогам. */
    public double forTransport(TransportType type) {
        return type == TransportType.PLANE ? straightKm : roadKm;
    }
}
