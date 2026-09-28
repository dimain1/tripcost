package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TransportType;

/**
 * Поезд.
 */
public class Train extends TicketTransport {

    public Train() {
        super(TransportType.TRAIN);
    }
}
