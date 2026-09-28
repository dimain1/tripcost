package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TransportType;

/**
 * Самолёт. Фиксированная часть тарифа — сборы аэропорта.
 */
public class Plane extends TicketTransport {

    public Plane() {
        super(TransportType.PLANE);
    }
}
