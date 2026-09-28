package ru.tripcost.model.transport;

/** Конкретный создатель: создаёт Taxi. */
public class TaxiFactory extends TransportFactory {

    @Override
    public Transport createTransport() {
        return new Taxi();
    }
}
