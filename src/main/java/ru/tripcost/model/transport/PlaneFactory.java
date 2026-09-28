package ru.tripcost.model.transport;

/** Конкретный создатель: создаёт Plane. */
public class PlaneFactory extends TransportFactory {

    @Override
    public Transport createTransport() {
        return new Plane();
    }
}
