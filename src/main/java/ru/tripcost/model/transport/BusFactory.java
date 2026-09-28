package ru.tripcost.model.transport;

/** Конкретный создатель: создаёт Bus. */
public class BusFactory extends TransportFactory {

    @Override
    public Transport createTransport() {
        return new Bus();
    }
}
