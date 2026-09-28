package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TransportType;

/**
 * Автобус.
 */
public class Bus extends TicketTransport {

    public Bus() {
        super(TransportType.BUS);
    }
}
